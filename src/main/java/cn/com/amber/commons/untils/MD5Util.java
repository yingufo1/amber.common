package cn.com.amber.commons.untils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5加密工具
 * 
 * @author yangying
 * 
 */
public class MD5Util {
	private static final Log log = LogFactory.getLog(MD5Util.class);
	private static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/**
	 * 加密字符串
	 * 
	 * @param s
	 * @return
	 */
	public static String MD5(String s) {
		try {
			MessageDigest mdInst = MessageDigest.getInstance("MD5");
			byte[] btInput = s.getBytes();
			// 使用指定的字节更新摘要
			mdInst.update(btInput);
			// 获得密文
			byte[] md = mdInst.digest();
			// 把密文转换成十六进制的字符串形式
			int j = md.length;
			char str[] = new char[j * 2];
			int k = 0;
			for (int i = 0; i < j; i++) {
				byte byte0 = md[i];
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];
				str[k++] = hexDigits[byte0 & 0xf];
			}
			return new String(str);
		} catch (Exception e) {
			log.error(e.getMessage());
			return null;
		}
	}

	public static String MD5File(File file) {
		try {
			InputStream fis = new FileInputStream(file);
			byte[] buffer = new byte[1024];
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			int numRead = 0;
			while ((numRead = fis.read(buffer)) > 0) {
				md5.update(buffer, 0, numRead);
			}
			fis.close();
			return toHexString(md5.digest());
		} catch (Exception e) {
			log.error(e.getMessage());
			return null;
		}
	}

	public static String toHexString(byte[] b) {
		StringBuilder sb = new StringBuilder(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			sb.append(hexDigits[(b[i] & 0xf0) >>> 4]);
			sb.append(hexDigits[b[i] & 0x0f]);
		}
		return sb.toString();
	}
	
	public static byte[] md5(byte[] in) {
		if(in==null)return null;
		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
			return md5.digest(in);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

}
