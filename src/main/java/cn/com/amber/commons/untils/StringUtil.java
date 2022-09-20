package cn.com.amber.commons.untils;

import org.apache.commons.lang3.StringUtils;

public class StringUtil {
	/**
	 * 检查指定字符串是否为空字符串
	 * <p>检查输入字符串为null或全为不可见字符
	 * @param in 输入字符串
	 * @return 若输入字符串为null或全为不可见字符则返回true，否则返回false
	 */
	public static boolean isBlankString(String in){
		return StringUtils.isBlank(in);
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
	
	public static String getHexStringFromBytes(byte[] in){
		if(in==null)return "";
		StringBuilder sb = new StringBuilder(in.length*2);
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
