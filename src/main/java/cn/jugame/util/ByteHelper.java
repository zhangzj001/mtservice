package cn.jugame.util;

public class ByteHelper {

	public static byte[] short2ByteArray(short num){
		byte[] result = new byte[2];
		result[0] = (byte) (num >>> 8);
		result[1] = (byte) (num);
		return result;
	}
	
	public static byte[] int2ByteArray(int num) {
		byte[] result = new byte[4];
		result[0] = (byte) (num >>> 24);// 取最�?位放�?下标
		result[1] = (byte) (num >>> 16);// 取次�?为放�?下标
		result[2] = (byte) (num >>> 8); // 取次�?位放�?下标
		result[3] = (byte) (num); // 取最�?位放�?下标
		return result;
	}
	
	public static int bytesToInt(byte b[], int offset) {
		int i = b.length - 1;
		int x = 0;
		for(; i>=0; i--){
			x |= (b[i] & 0xff) << (8*(b.length-i-1));
		}
		return x;
    }
	public static long bytesToLong(byte[] b, int offset){
		int i = b.length - 1;
		long x = 0;
		for(; i>=0; i--){
			x |= (b[i] & 0xffL) << (8*(b.length-i-1));
		}
		return x;
	}
	
	public static int bytesToInt(byte b[]){
		return bytesToInt(b, 0);
	}

	public static byte[] long2ByteArray(long num) {
		byte[] result = new byte[8];
		result[0] = (byte) (num >>> 56);
		result[1] = (byte) (num >>> 48);
		result[2] = (byte) (num >>> 40);
		result[3] = (byte) (num >>> 32);
		result[4] = (byte) (num >>> 24);
		result[5] = (byte) (num >>> 16);
		result[6] = (byte) (num >>> 8);
		result[7] = (byte) (num);
		return result;
	}
	
	public static short bytesToShort(byte[] b, int offset){
		return (short)(b[offset + 1] & 0xff | (b[offset] & 0xff) << 8);
	}
	
	public static short bytesToShort(byte[] b){
		return bytesToShort(b, 0);
	}
	
	public static String byte2HexString(byte b) {
		return String.format("%02x", b);
	}
	
	 /* Convert byte[] to hex string.这里我们可以将byte转换成int，然后利用Integer.toHexString(int)来转换成16进制字符串�?  
	 * @param src byte[] data  
	 * @return hex string  
	 */     
	public static String bytesToHexString(byte[] src){  
	    StringBuilder stringBuilder = new StringBuilder("");  
	    if (src == null || src.length <= 0) {  
	        return null;  
	    }  
	    for (int i = 0; i < src.length; i++) {  
	        int v = src[i] & 0xFF;  
	        String hv = Integer.toHexString(v);  
	        if (hv.length() < 2) {  
	            stringBuilder.append(0);  
	        }  
	        stringBuilder.append(hv);  
	    }  
	    return stringBuilder.toString();  
	}  

	public static void printBytes(byte[] byteArray) {
		for (int i = 0; i < byteArray.length; i++) {
			System.out.print(byte2HexString(byteArray[i]));
			System.out.print(" ");
		}
		System.out.println();
	}
	
	public static byte[] getBytes(char[] chars) {
		int length = chars.length;
		byte[] data = new byte[length];
		for (int i = 0; i < length; i++) {
			data[i] = (byte) chars[i];
		}

		return data;
	}
	
	public static byte[] concatByteArrays(byte[] ... arrays) {
		int totalLength = 0;
		for(byte[] array : arrays) {
			totalLength += array.length;
		}
		
		byte[] newArray = new byte[totalLength];
		int i=0;
		for (byte[] array : arrays) {
			for (byte item : array) {
				newArray[i++] = item;
			}
		}
		return newArray;
	}
	
	
	public static byte[] hexStringToBytes(String hexString) {  
	    if (hexString == null || hexString.equals("")) {  
	        return null;  
	    }  
	    hexString = hexString.toUpperCase();  
	    int length = hexString.length() / 2;  
	    char[] hexChars = hexString.toCharArray();  
	    byte[] d = new byte[length];  
	    for (int i = 0; i < length; i++) {  
	        int pos = i * 2;  
	        d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));  
	    }  
	    return d;  
	}  
	
	private static byte charToByte(char c) {  
	    return (byte) "0123456789ABCDEF".indexOf(c);  
	}
	
	public static boolean bytes_equals(byte[] b1, byte[] b2){
		if(b1 == null && b2 == null)
			return true;
		if(b1 == null && b2 != null)
			return false;
		if(b1 != null && b2 == null)
			return false;
		
		if(b1.length != b2.length)
			return false;
		for(int i=0; i<b1.length; ++i){
			if(b1[i] != b2[i])
				return false;
		}
		return true;
	}
	
	public static void main(String[] args) {

		int x = bytesToInt(new byte[]{(byte)0x00, (byte)0x00, (byte)0x04});
		System.out.println(x);
		
//		int userNameLen = 10;
//		int passwordLen = 6;
		
//		long x = 0xae000000e8L; // E8
//		x += userNameLen + passwordLen;
//		x = x << 24 | 0x0000000000ffffff;
//		printBytes(long2ByteArray(x));
//		
//		byte[] midBytes = long2ByteArray(0xffffffffff03ffffL);
//		printBytes(midBytes);
//
//		byte[] bytes = int2ByteArray(248 | 0xffff0000); // D9
//		printBytes(bytes);
//		
//		byte[] testint = {0x02};
//		System.out.println(testint[0]);
	}

}
