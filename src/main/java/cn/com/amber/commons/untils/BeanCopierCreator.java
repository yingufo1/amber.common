package cn.com.amber.commons.untils;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class BeanCopierCreator {
	private static Method defineClassMethodLoader = null;
	private static ClassLoader classLoader = null;
	private static int nameIndex;
	static{
		try {
			classLoader = Thread.currentThread().getContextClassLoader();//BeanCopierCreator.class.getClassLoader();
			defineClassMethodLoader = ClassLoader.class.getDeclaredMethod("defineClass", String.class,byte[].class,int.class,int.class);
			defineClassMethodLoader.setAccessible(true);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e){
			e.printStackTrace();
		}
	}
	
	public static BeanCopier create(Class<?> src,Class<?> des){
		String classNamePath = getClassName(src,des);
		String className = classNamePath.replaceAll("/", ".");
		Class<?> clazz = null;
		try {
			clazz = classLoader.loadClass(className);
		} catch (ClassNotFoundException e1) {
			ClassWriter cw = new ClassWriter(0);
			cw.visit(Opcodes.V1_2, Opcodes.ACC_PUBLIC, 
					classNamePath, null,
					Type.getInternalName(BeanCopier.class), null);
			cw.visitSource("<generated>", null);
			MethodVisitor constractorVisitor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
			constractorVisitor.visitVarInsn(Opcodes.ALOAD, 0);
			constractorVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(BeanCopier.class) , "<init>", "()V");
			constractorVisitor.visitInsn(Opcodes.RETURN);
			constractorVisitor.visitMaxs(1, 1);
			constractorVisitor.visitEnd();
			MethodVisitor copyVisitor = cw.visitMethod(Opcodes.ACC_PUBLIC, "copy", "(Ljava/lang/Object;Ljava/lang/Object;)V", null, null);
			//载入两个输入参数至堆栈
			copyVisitor.visitVarInsn(Opcodes.ALOAD, 2);
			copyVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(des));
			copyVisitor.visitVarInsn(Opcodes.ALOAD, 1);
			copyVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(src));
			
			//逐个解析setter
			Set<Method> setters = new HashSet<Method>();
			for(Method m:des.getMethods()){
				if(m.getName().startsWith("set")&&m.getParameterTypes().length==1&&m.getReturnType().equals(Void.TYPE)){
					setters.add(m);
				}
			}
			//查找能与setter对应的getter
			for(Method m:src.getMethods()){
				if(m.getName().startsWith("get")&&m.getParameterTypes().length==0&&!m.getReturnType().equals(Void.TYPE)){
					String setterName = m.getName().replaceFirst("get", "set");
					for(Method setter:setters){
						if(setter.getName().equals(setterName)){
							genCopyPropertyByteCode(m, setter, copyVisitor);
							
						}
					}
				}
				if(m.getName().startsWith("is")&&m.getParameterTypes().length==0&&(m.getReturnType().equals(Boolean.TYPE)||m.getReturnType().equals(Boolean.class))){
					String setterName = m.getName().replaceFirst("is", "set");
					for(Method setter:setters){
						if(setter.getName().equals(setterName)){
							genCopyPropertyByteCode(m, setter, copyVisitor);
						}
					}
				}
			}
			copyVisitor.visitInsn(Opcodes.RETURN);
			copyVisitor.visitMaxs(4, 3);
			copyVisitor.visitEnd();
			cw.visitEnd();
			byte[] b = cw.toByteArray();//new java.io.FileOutputStream("target\\classes\\newClass.class",false).write(b)
			try {
				clazz = (Class<?>)defineClassMethodLoader.invoke(classLoader, className, b , 0 , b.length);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		try {
			BeanCopier beanCopier = (BeanCopier) clazz.newInstance();
			return beanCopier;
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (InstantiationException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static synchronized String getClassName(Class<?> src,Class<?> des){
		String namePerfix = Type.getInternalName(BeanCopier.class);
		//src和des中应该只会有一个是匿名且无public访问域类
		if(!Modifier.isPublic(src.getModifiers())){
			namePerfix = Type.getInternalName(src);
		}
		if(!Modifier.isPublic(des.getModifiers())){
			namePerfix = Type.getInternalName(des);
		}
		String className = namePerfix + "$copyier$" + Integer.toHexString(System.identityHashCode(src)) + "$to$" + Integer.toHexString(System.identityHashCode(des))+"$"+Integer.toHexString(nameIndex++);
		Class<?> clazz = null;
		try {
			clazz = classLoader.loadClass(className);
		} catch (ClassNotFoundException e1) {
			return className;
		}
		for(int i=0;clazz!=null;i++){
			try {
				//理论上不会重复，但是还是处理一下
				clazz = classLoader.loadClass(className+"$"+i);
			} catch (ClassNotFoundException e) {
				return className+"$"+i;
			}
		}
		return className;
	}
	
	
	private static void genCopyPropertyByteCode(Method getter,Method setter,MethodVisitor mv){
		Class<?> setterType = setter.getParameterTypes()[0];
		Class<?> getterType = getter.getReturnType();
		if(getterType.isAssignableFrom(setterType)){
			genCasterByteCode(mv, getter, setter, null, null, null, null, null);
		}else{
			boolean genCast = false;
			Integer primitiveCastOpcode = null;
			Integer invokeOpcode = null;
			String invokeClass = null;
			String invokeMethod = null;
			String invokeFuncArg = null;
			if(Byte.class.equals(getterType)){
				if(setterType.isPrimitive()){//Byte -> Byte 在最开始已经被处理了，下同
					invokeOpcode = Opcodes.INVOKEVIRTUAL;
					invokeClass = Type.getInternalName(Byte.class);
					invokeMethod = "byteValue";
					invokeFuncArg = "()B";
					if(Byte.TYPE.equals(setterType)||Integer.TYPE.equals(setterType)){
						//Byte -> byte or Byte-> int
						genCast = true;
					}else if(Long.TYPE.equals(setterType)){
						//Byte -> long
						primitiveCastOpcode = Opcodes.I2L;
						genCast = true;
					}
				}
			}else if(Byte.TYPE.equals(getterType)){
				if(Byte.TYPE.equals(setterType)||Integer.TYPE.equals(setterType)){
					//byte -> byte or byte-> int
					genCast = true;
				}else if(Long.TYPE.equals(setterType)){
					//byte -> long
					primitiveCastOpcode = Opcodes.I2L;
					genCast = true;
				}else if(Byte.class.equals(setterType)||Integer.class.equals(setterType)){
					//byte -> Byte or byte -> Integer
					invokeOpcode = Opcodes.INVOKESTATIC;
					invokeClass = Type.getInternalName(Byte.class);
					invokeMethod = "valueOf";
					invokeFuncArg = "(B)Ljava/lang/Byte;";
					genCast = true;
				}else if(Long.class.equals(setterType)){
					//byte -> Long
					invokeOpcode = Opcodes.INVOKESTATIC;
					invokeClass = Type.getInternalName(Byte.class);
					invokeMethod = "valueOf";
					invokeFuncArg = "(B)Ljava/lang/Byte;";
					primitiveCastOpcode = Opcodes.I2L;
					genCast = true;
				}
			}else if(Character.class.equals(getterType)){
				if(setterType.isPrimitive()){
					invokeOpcode = Opcodes.INVOKEVIRTUAL;
					invokeClass = Type.getInternalName(Character.class);
					invokeMethod = "charValue";
					invokeFuncArg = "()C";
					if(Character.TYPE.equals(setterType)||Integer.TYPE.equals(setterType)){
						//Character -> char or Character-> int
						genCast = true;
					}else if(Long.TYPE.equals(setterType)){
						//Character -> long
						primitiveCastOpcode = Opcodes.I2L;
						genCast = true;
					}
				}
			}else if(Character.TYPE.equals(getterType)){
				if(Character.TYPE.equals(setterType)||Integer.TYPE.equals(setterType)){
					//char -> char or char-> int
					genCast = true;
				}else if(Long.TYPE.equals(setterType)){
					//char -> long
					primitiveCastOpcode = Opcodes.I2L;
					genCast = true;
				}else if(Character.class.equals(setterType)||Integer.class.equals(setterType)){
					//char -> Character or char -> Integer
					invokeOpcode = Opcodes.INVOKESTATIC;
					invokeClass = Type.getInternalName(Character.class);
					invokeMethod = "valueOf";
					invokeFuncArg = "(C)Ljava/lang/Character;";
					genCast = true;
				}else if(Long.class.equals(setterType)){
					//char -> Long
					invokeOpcode = Opcodes.INVOKESTATIC;
					invokeClass = Type.getInternalName(Character.class);
					invokeMethod = "valueOf";
					invokeFuncArg = "(C)Ljava/lang/Character;";
					primitiveCastOpcode = Opcodes.I2L;
					genCast = true;
				}
			}else if(Integer.class.equals(getterType)){
				if(setterType.isPrimitive()){
					invokeOpcode = Opcodes.INVOKEVIRTUAL;
					invokeClass = Type.getInternalName(Integer.class);
					invokeMethod = "intValue";
					invokeFuncArg = "()I";
					if(Integer.TYPE.equals(setterType)){
						//Integer -> int
						genCast = true;
					}else if(Long.TYPE.equals(setterType)){
						//Integer -> long
						primitiveCastOpcode = Opcodes.I2L;
						genCast = true;
					}
				}
			}else if(Integer.TYPE.equals(getterType)){
				if(Integer.TYPE.equals(setterType)){
					//int -> int
					genCast = true;
				}else if(Long.TYPE.equals(setterType)){
					//int -> long
					primitiveCastOpcode = Opcodes.I2L;
					genCast = true;
				}else if(Integer.class.equals(setterType)){
					//int -> Integer
					invokeOpcode = Opcodes.INVOKESTATIC;
					invokeClass = Type.getInternalName(Integer.class);
					invokeMethod = "valueOf";
					invokeFuncArg = "(I)Ljava/lang/Integer;";
					genCast = true;
				}else if(Long.class.equals(setterType)){
					//int -> Long
					invokeOpcode = Opcodes.INVOKESTATIC;
					invokeClass = Type.getInternalName(Integer.class);
					invokeMethod = "valueOf";
					invokeFuncArg = "(I)Ljava/lang/Integer;";
					primitiveCastOpcode = Opcodes.I2L;
					genCast = true;
				}
			}else if(Long.class.equals(getterType)){
				if(Long.TYPE.equals(setterType)){
					//Long -> long
					invokeOpcode = Opcodes.INVOKEVIRTUAL;
					invokeClass = Type.getInternalName(Long.class);
					invokeMethod = "longValue";
					invokeFuncArg = "()J";
					genCast = true;
				}
			}else if(Long.TYPE.equals(getterType)){
				if(Long.class.equals(setterType)){
					//long -> Long
					invokeOpcode = Opcodes.INVOKESTATIC;
					invokeClass = Type.getInternalName(Long.class);
					invokeMethod = "valueOf";
					invokeFuncArg = "(J)Ljava/lang/Long";
					genCast = true;
				}
			}else if(Boolean.class.equals(getterType)){
				if(Boolean.TYPE.equals(setterType)){
					//Boolean -> boolean
					invokeOpcode = Opcodes.INVOKEVIRTUAL;
					invokeClass = Type.getInternalName(Boolean.class);
					invokeMethod = "booleanValue";
					invokeFuncArg = "()Z";
					genCast = true;
				}
			}else if(Boolean.TYPE.equals(getterType)){
				if(Long.class.equals(setterType)){
					//boolean -> Boolean
					invokeOpcode = Opcodes.INVOKESTATIC;
					invokeClass = Type.getInternalName(Boolean.class);
					invokeMethod = "valueOf";
					invokeFuncArg = "(Z)Ljava/lang/Boolean";
					genCast = true;
				}
			}
			if(genCast){
				genCasterByteCode(mv, getter, setter, primitiveCastOpcode, invokeOpcode, invokeClass, invokeMethod, invokeFuncArg);
			}
		}
	}
	
	private static void genCasterByteCode(MethodVisitor mv,Method getter,Method setter,Integer primitiveCastOpcode,Integer opcode,String className,String functionName,String functionArgs){
		mv.visitInsn(Opcodes.DUP2);
		mv.visitMethodInsn(getter.getDeclaringClass().isInterface()?Opcodes.INVOKEINTERFACE:Opcodes.INVOKEVIRTUAL, Type.getInternalName(getter.getDeclaringClass()), getter.getName(), Type.getMethodDescriptor(getter));
		if(opcode!=null){
			mv.visitMethodInsn(opcode, className, functionName, functionArgs);
		}
		if(primitiveCastOpcode!=null){
			mv.visitInsn(primitiveCastOpcode);
		}
		mv.visitMethodInsn(getter.getDeclaringClass().isInterface()?Opcodes.INVOKEINTERFACE:Opcodes.INVOKEVIRTUAL, Type.getInternalName(setter.getDeclaringClass()), setter.getName(), Type.getMethodDescriptor(setter));
	}
}
