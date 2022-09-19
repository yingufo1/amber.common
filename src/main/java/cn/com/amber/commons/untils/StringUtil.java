package cn.com.amber.commons.untils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringUtil {
	/**
	 * 检查指定字符串是否为空字符串
	 * <p>检查输入字符串为null或全为不可见字符
	 * @param in 输入字符串
	 * @return 若输入字符串为null或全为不可见字符则返回true，否则返回false
	 */
	public static boolean isBlankString(String in){
		return in==null?true:in.trim().length()==0?true:false;
	}
	
	/**
	 * 返回空字符串或做过trim的字符串
	 * @param in 输入字符串
	 * @return 若输入字符串为null或字符串不含可见字符，则返回null，否则返回trim的结果
	 */
	public static String getNullOrTrimString(String in){
		if(in==null){
			return null;
		}
		String t = in.trim();
		if(t.length()==0){
			return null;
		}
		return t;
	}
	
	public static String getEncryptPswd(String pswd){
		/*
		 * 密码hash加密
		 * 明文以UTF8转码转为sha1后，再接一遍明文转UTF8，再转为md5，
		 * 之后将其转换为十六进制表示的字符串
		 */
		MessageDigest md5 = null;
		MessageDigest sha = null;
		try {
			md5 = MessageDigest.getInstance("md5");
			sha = MessageDigest.getInstance("sha1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		byte[] data = null;
		try {
			data = pswd.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		byte[] tdata1 = sha.digest(data);
		byte[] tdata2 = new byte[data.length+tdata1.length];
		System.arraycopy(data, 0, tdata2, 0, data.length);
		System.arraycopy(tdata1, 0, tdata2, data.length, tdata1.length);
		byte[] outdata = md5.digest(tdata2);
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<outdata.length;i++){
			sb.append(String.format("%1$02X", outdata[i]&0xFF));
		}
		return sb.toString();
	}
	
	public static String getHexStringFromBytes(byte[] in){
		if(in==null)return "";
		StringBuffer sb = new StringBuffer(in.length*2);
		for(byte b:in){
			sb.append(String.format("%1$02X", b&0xFF));
		}
		return sb.toString();
	}
	
	public static String getHexLogFromBytes(byte[] in){
		StringBuffer wholeSb = new StringBuffer("\n");
		StringBuffer byteSb = null;
		StringBuffer textSb = null;
		for(int i=0;i<in.length;i++){
			if(i%16==0){
				appendSb(wholeSb,byteSb,textSb);
				byteSb = new StringBuffer();
				byteSb.append(String.format("%1$04X:", i));
				textSb = new StringBuffer();
			}else if(i%8==0){
				byteSb.append(' ');
				textSb.append(' ');
			}
			byteSb.append(String.format(" %1$02X", in[i]&0xFF));
			textSb.append(in[i]>=32?in[i]<=126?(char)in[i]:'?':'?');
		}
		appendSb(wholeSb,byteSb,textSb);
		return wholeSb.toString();
	}
	
	private static void appendSb(StringBuffer allSb,StringBuffer byteSb,StringBuffer textSb){
		if(byteSb==null||textSb==null)return;
		allSb.append(byteSb);
		int i=byteSb.length();
		while(i++<54){
			allSb.append(' ');
		}
		allSb.append(':');
		allSb.append(textSb);
		allSb.append('\n');
	}
}
