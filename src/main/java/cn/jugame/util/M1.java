package cn.jugame.util;

import java.util.Random;

public class M1 {

	//16个加密数
	private static final int [] ENCODE_KEY = {0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
		0xf0, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7};
	
	//加密
	public static final byte[] encode(byte[] aSrc) {
		int[] sKey = ENCODE_KEY;

		int[] sMask = new int[8];
		System.arraycopy(sKey, 0, sMask, 0, 8);

		int[] sInc = new int[8];
		System.arraycopy(sKey, 8, sInc, 0, 8);

		byte[] sRnd = new byte[8];
		int sRand = new Random(System.currentTimeMillis()).nextInt();
		sRnd[0] = (byte) (sRand >> 24 & 0xFF);
		sRnd[1] = (byte) (sRand >> 16 & 0xFF);
		sRnd[2] = (byte) (sRand >> 8 & 0xFF);
		sRnd[3] = (byte) (sRand & 0xFF);

		sRnd[4] = (byte) ((sRnd[0] + 87) % 256);
		sRnd[5] = (byte) ((sRnd[1] + 29) % 256);
		sRnd[6] = (byte) ((sRnd[2] + 171) % 256);
		sRnd[7] = (byte) ((sRnd[3] + 148) % 256);

		int sLength = aSrc.length;
		byte[] sDest = new byte[sLength + 18];
		
		//jhw
		sDest[0] = 106;
		sDest[1] = 104;
		sDest[2] = 119;
		sDest[3] = 0;
		
		//版本信息
		sDest[4] = 0;
		sDest[5] = 0;
		sDest[6] = 1;
		sDest[7] = 1;
		
		//冗余位
		sDest[8] = 0;
		sDest[9] = 0;
		sDest[10] = 0;
		sDest[11] = 0;
		
		//数据
		sDest[12] = sRnd[0];
		sDest[13] = sRnd[1];
		sDest[14] = sRnd[2];
		sDest[15] = sRnd[3];

		int sMaskCheck = 0;

		for (int j = 0; j < sLength; j++) {
			if (j % 8 == 0) {
				sMask[0] = ((sMask[0] + sInc[0] + sRnd[0]) % 256);
				sMask[1] = ((sMask[1] + sInc[1] + sRnd[1]) % 256);
				sMask[2] = ((sMask[2] + sInc[2] + sRnd[2]) % 256);
				sMask[3] = ((sMask[3] + sInc[3] + sRnd[3]) % 256);
				sMask[4] = ((sMask[4] + sInc[4] + sRnd[4]) % 256);
				sMask[5] = ((sMask[5] + sInc[5] + sRnd[5]) % 256);
				sMask[6] = ((sMask[6] + sInc[6] + sRnd[6]) % 256);
				sMask[7] = ((sMask[7] + sInc[7] + sRnd[7]) % 256);
			}

			int sTempA = aSrc[j] & 0xFF;
			int sTempB = sTempA ^ sMask[(j % 8)];
			sDest[(16 + j)] = (byte) (sTempB & 0xFF);
			sMaskCheck ^= sTempA;
//			
//			System.out.println("encode,\tsmark: "+ sMask[(j % 8)] + "\tsTempb" + sTempB + ",\taSrc:"+ aSrc[j] + ",\t dest:" + sDest[j+16] + ",\tj:" +j);
		}

		sDest[(16 + sLength)] = (byte) (0xFF & (sMaskCheck ^ sMask[0]));
		sDest[(16 + sLength + 1)] = (byte) (0xFF & (sMaskCheck ^ sMask[1]));

		return sDest;
	}

	//解密
	public static final int decode(byte[] aSrc, StringBuffer dest) {
		if (aSrc == null) {
			return -1;
		}
		int sLength = aSrc.length;
		
		byte[] bSrc = new byte[sLength];
		
		System.arraycopy(aSrc, 0, bSrc, 0, sLength);

		if ((sLength < 18) || (bSrc[0] != 106) || (bSrc[1] != 104) || (bSrc[2] != 119)) {
			return -2;
		}

		int[] sKey = ENCODE_KEY;

		int[] sMask = new int[8];
		System.arraycopy(sKey, 0, sMask, 0, 8);

		int[] sInc = new int[8];
		System.arraycopy(sKey, 8, sInc, 0, 8);

		byte[] rnd = new byte[8];
		rnd[0] = bSrc[12];
		rnd[1] = bSrc[13];
		rnd[2] = bSrc[14];
		rnd[3] = bSrc[15];

		rnd[4] = (byte) ((rnd[0] + 87) % 256);
		rnd[5] = (byte) ((rnd[1] + 29) % 256);
		rnd[6] = (byte) ((rnd[2] + 171) % 256);
		rnd[7] = (byte) ((rnd[3] + 148) % 256);

		int sMaskCheck = 0;

		for (int j = 16; j < sLength - 2; j++) {
			if (j % 8 == 0) {
				sMask[0] = ((sMask[0] + sInc[0] + rnd[0]) % 256);
				sMask[1] = ((sMask[1] + sInc[1] + rnd[1]) % 256);
				sMask[2] = ((sMask[2] + sInc[2] + rnd[2]) % 256);
				sMask[3] = ((sMask[3] + sInc[3] + rnd[3]) % 256);
				sMask[4] = ((sMask[4] + sInc[4] + rnd[4]) % 256);
				sMask[5] = ((sMask[5] + sInc[5] + rnd[5]) % 256);
				sMask[6] = ((sMask[6] + sInc[6] + rnd[6]) % 256);
				sMask[7] = ((sMask[7] + sInc[7] + rnd[7]) % 256);
			}
			
			int sTempA = bSrc[j];
			int sTempB = sTempA ^ sMask[(j % 8)];
			bSrc[j] = (byte) (sTempB & 0xFF);
			sMaskCheck ^= sTempB;
//			
//			System.out.println("decode,\tsmask:" + sMask[(j % 8)] +",\ttmpB:" + sTempB + ", aSrc:"+ aSrc[j] + ", dest:" +bSrc[j] + ",j:" +j);
		}

		if ((bSrc[(sLength - 2)] != (byte) (0xFF & (sMaskCheck ^ sMask[0])))
				|| (bSrc[(sLength - 1)] != (byte) (0xFF & (sMaskCheck ^ sMask[1])))) {
			return -3;
		}
		
		dest.setLength(0);
		dest.append(new String(bSrc, 16, bSrc.length - 18));
		return 0;
	}
	
	public static boolean isEncoded(byte[] aSrc)
	{
		if(aSrc == null) {
			return false;
		}
		
		if(aSrc.length < 2) {
			return false;
		}
		
		if((byte) aSrc[0] == 106 && (byte) aSrc[1] == 104 && (byte) aSrc[2] == 119) {
			return true;
		}

		return false;
	}
	
	public static void main(String[] args) throws Exception{
//		String a = "6a6877000000010100000000df836390a4a703f74acb3808d584903b1b5a6c3b140e74bea8f6571b2993486ece0c41ea6821ded0c4d208f78eeb2a1424a31f89810d54900a1803e4a493dd4a09d814d52c076cdec736183014ab550494c559449868f0d0c0467a3de8a36d5b6574282fb1cf189840c3684c4028950c4e0b60a313f12fa5f2f2688e693c072a8e5564f3fdf58effe6ca6ed5aa093d7e74e6338e049752fd021654280a128279688f570038e966bde22b06749d5d4d1b87d35e59d513d0ace02f3bc2318d673466644ce335002c8614944e8a6aabd112010d4ae5d5316485e1f6c3c08ef76f5dee58bc3cb30f80acecceb95a36cc661e69fff77b10336dd20602f42ed2ab90203cc0ff0dd77054b23568b6b0e10a7e438abef0c92f82d898f05880ab44433c5dcf3896d3cbe56a3216ae88d2e83ad9a76a119d2ba0fe413938f88b052c721fb9921ef37c0e949f3d8db48853355d45a0c9b29cf8a8ca92";
		
		String a = "6A68770000000101000000006FCAF3DF14EE9384BA9088BBB5FAF095FBA84C9D4421A4AB788BA726E98F882A8EE001D698CCEE5F944DD8402E514ACE440DBFA3911E64E91A2913CD24CB5DC28990945D1C867C05D7B568EBF46D3572748FB95A487FA07D30B34AD8A8F7AD87A500687BC1AAC89FF024B843600AB5BE6E2D00D1832A5F0482DBF86F69AC075A8EE564836D4C7E3C5681DE168A071DE054E413F874C82298D25B24C54A9EC24DA873976CE814B622324456F37D77ED5127CDFE03E5B0C085F08E2BDBB1E5E7ACE6DCCC7B25311CCD04A7BE41CA7D3114A1572AEB25B65458D173F3754EB32F312E1C7C98E37A309B1C39E965569E86DC89A917B9A098FDA3B69B649FD24B90003CE0FF2D2759C461455306E3C1945ECDEA2CD04F7FCD08ADA00570F6047F7CB90F34D6CFFB281A5DC6D1584508A0791D8A9F3DE1B0CDB12028A99B0CAC0A9FD1123673947E358F869D17F82895FBE5B6A9187C06781A65";
		
//		byte[] encode = encode(a.getBytes());
		
		byte[] encode = ByteHelper.hexStringToBytes(a);
		StringBuffer dest = new StringBuffer();
		int decodeRtn = decode(encode, dest);
		
		
		System.out.println("decode rtn:" + decodeRtn  + ", dest:" + dest.toString());
		
		
//		String x = "{\"data\":{\"password\":\"233157\",\"account\":\"10002151\"},\"service\":\"user.login\",\"id\":1401076739560,\"game\":{\"cpId\":2215,\"gameId\":8},\"client\":{\"ve\":\"A-1.0.2\",\"ex\":\"imei:024209980474017|imsi:310260634646133|model:GT-N7000|net:WIFI|mobi:|resX:640|resY:400|mac:A0:61:B9:70:3F:83|density:1.0\",\"fr\":\"ShuizhuSDK\",\"os\":\"android\",\"ch\":\"3100001000\",\"si\":\"329CD1B091DCE26CE39103CF293DA115\"}}";
//		HttpFetcher fetcher = new HttpFetcher();
//		byte[] bs = fetcher.http_post("http://sdk.shuizhu123.com/sdkapi/sdk_service.jsp", M1.encode(x.getBytes("UTF-8")));
//		
//		System.out.println(ByteHelper.bytesToHexString(bs));
	}
}
