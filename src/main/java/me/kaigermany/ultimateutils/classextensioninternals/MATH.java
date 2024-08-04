package me.kaigermany.ultimateutils.classextensioninternals;

public interface MATH {
	/**
	 * Branchless implementation of Math.abs()
	 * @param value some number
	 * @return positive-only number
	 */
	default int abs(int value){
		return (value ^ (value >> 31)) - (value >> 31);
	}
	/**
	 * Branchless implementation of Math.abs()
	 * @param value some number
	 * @return positive-only number
	 */
	default long abs(long value){
		return (value ^ (value >> 63)) - (value >> 63);
	}
	/**
	 * Alternative implementation of Math.abs()
	 * @param value some number
	 * @return positive-only number
	 */
	default float abs(float value) {
		if(value < 0) return -value;
		return value;
	}
	/**
	 * Alternative implementation of Math.abs()
	 * @param value some number
	 * @return positive-only number
	 */
	default double abs(double value) {
		if(value < 0) return -value;
		return value;
	}

	/**
	 * Branchless implementation of Math.max()
	 * @param a some number
	 * @param b some number
	 * @return the biggest value a or b
	 */
	default int max(int a, int b) {
		return a + (
				//if a < 0 && b >= 0 -> true;
				//else if signs are equal: use sign of (b - a)
				( (
					(a | ~b) & ( (a ^ b) | ~(b - a) )
				  ) >> 31)
				&
				(b - a));
	}
	/**
	 * Branchless implementation of Math.max()
	 * @param a some number
	 * @param b some number
	 * @return the biggest value a or b
	 */
	default long max(long a, long b) {
		return a + (
				//if a < 0 && b >= 0 -> true;
				//else if signs are equal: use sign of (b - a)
				( (
					(a | ~b) & ( (a ^ b) | ~(b - a) )
				  ) >> 63)
				&
				(b - a));
		/*
	    return a + (

	            ~(( 
	                ( 
	                  (a >> 31) ^ (b >> 31)) &
	                  (~(a >> 31)) & ((b >> 31))
	                ) | ( 
	                  ~((a >> 31) ^ (b >> 31)) &
	                  ((b - a) >> 31)
	              ))
	            
	            
	            & (b - a));
	            */
	}
	/**
	 * Alternative implementation of Math.max()
	 * @param a some number
	 * @param b some number
	 * @return the biggest value a or b
	 */
	default float max(float a, float b) {
		if(a > b) return a;
		return b;
	}
	/**
	 * Alternative implementation of Math.max()
	 * @param a some number
	 * @param b some number
	 * @return the biggest value a or b
	 */
	default double max(double a, double b) {
		if(a > b) return a;
		return b;
	}
	/**
	 * Branchless implementation of Math.min()
	 * @param a some number
	 * @param b some number
	 * @return the smallest value a or b
	 */
	default int min(int a, int b) {
		//return a + (((b - a) >> 31) & (b - a));
		return a + (
				( (
					(b | ~a) & ( (b ^ a) | ~(a - b) )
				  ) >> 31)
				&
				(b - a));
	}

	/**
	 * Branchless implementation of Math.min()
	 * @param a some number
	 * @param b some number
	 * @return the smallest value a or b
	 */
	default long min(long a, long b) {
		//return a + (((b - a) >> 31) & (b - a));
		return a + (
				( (
					(b | ~a) & ( (b ^ a) | ~(a - b) )
				  ) >> 63)
				&
				(b - a));
	}

	/**
	 * Alternative implementation of Math.min()
	 * @param a some number
	 * @param b some number
	 * @return the smallest value a or b
	 */
	default float min(float a, float b) {
		if(a < b) return a;
		return b;
	}

	/**
	 * Alternative implementation of Math.min()
	 * @param a some number
	 * @param b some number
	 * @return the smallest value a or b
	 */
	default double min(double a, double b) {
		if(a < b) return a;
		return b;
	}


	/**
	 * Normal rounding.
	 * returns Math.round() as long value.
	 * @param val some number
	 * @return Math.round(val) as long
	 */
	default long round(double val) {
		return Math.round(val);
	}
	/**
	 * Rounding down.
	 * returns Math.floor() as long value.
	 * @param val some number
	 * @return Math.floor(val) as long
	 */
	default long floor(double val) {
		return (long)Math.floor(val);
	}
	/**
	 * Rounding up.
	 * returns Math.ceil() as long value.
	 * @param val some number
	 * @return Math.ceil(val) as long
	 */
	default long ceil(double val) {
		return (long)Math.ceil(val);
	}
}
