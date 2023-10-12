package me.kaigermany.ultimateutils.classextensioninternals;

public interface FastHex {
	static char[] HEX_CHARACTERS = "0123456789ABCDEF".toCharArray();
	
	default String hexByte(int val){
		return new String(new char[] {
				HEX_CHARACTERS[(val >>> 4) & 0xF],
				HEX_CHARACTERS[val & 0xF]
		});
	}
	
	default String hexInt(int val){
		return new String(new char[] {
				HEX_CHARACTERS[val >>> 28],
				HEX_CHARACTERS[(val >>> 24) & 0xF],
				
				HEX_CHARACTERS[(val >>> 20) & 0xF],
				HEX_CHARACTERS[(val >>> 16) & 0xF],
				
				HEX_CHARACTERS[(val >>> 12) & 0xF],
				HEX_CHARACTERS[(val >>> 8) & 0xF],
				
				HEX_CHARACTERS[(val >>> 4) & 0xF],
				HEX_CHARACTERS[val & 0xF]
		});
	}
	
	default String hex(byte[] data) {
		char[] out = new char[data.length << 1];
		for(int i=0; i<data.length; i++) {
			out[i << 1] = HEX_CHARACTERS[(data[i] >>> 4) & 0xF];
			out[(i << 1) | 1] = HEX_CHARACTERS[data[i] & 0xF];
		}
		return new String(data);
	}
	
	default byte[] unhex(char[] in) {
		/*
		if(chr >>> 4 == 3) chr &= 15;
		else if(((chr >>> 3) | 4) == 12) chr = 9 + (chr & 7);
		else chr = 0;
		*/
		byte[] out = new byte[in.length >> 1];
		for(int i=0; i<out.length; i++) {
			out[i] = (byte)( (unhexChar(in[i << 1]) << 4) | unhexChar(in[(i << 1) | 1]) );
		}
		return out;
	}
	
	default int unhexChar(int chr) {
		/*
		if(chr >>> 4 == 3) chr &= 15;
		else if(((chr >>> 3) | 4) == 12) chr = 9 + (chr & 7);
		else chr = 0;
		*/
		/*
		if(isNull((chr >>> 4) - 3)) chr &= 15;
		else if(isNull(((chr >>> 3) | 4) - 12)) chr = 9 + (chr & 7);
		else chr = 0;
		*/
		
		return 
				(isNull((chr >>> 4) - 3) & chr & 15)
			| 
				(isNull(((chr >>> 3) | 4) - 12) & (9 + (chr & 7)));
	}
	
	default int isNull(int a){// true == -1, false == 0.
		return ~(((-a) >> 31) | (a >> 31));
	}
}
