package me.kaigermany.ultimateutils.image.dds;

public class DDSVec {
	private float x;
	private float y;
	private float z;

	public DDSVec() {
	}

	public DDSVec(final float a) {
		this(a, a, a);
	}

	public DDSVec(final float a, final float b, final float c) {
		x = a;
		y = b;
		z = c;
	}

	public float x() {
		return x;
	}

	public float y() {
		return y;
	}

	public float z() {
		return z;
	}

	public DDSVec set(final float a) {
		this.x = a;
		this.y = a;
		this.z = a;

		return this;
	}

	public DDSVec set(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;

		return this;
	}

	public DDSVec set(final DDSVec v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;

		return this;
	}

	public DDSVec add(final DDSVec v) {
		x += v.x;
		y += v.y;
		z += v.z;

		return this;
	}

	public DDSVec add(final float x, final float y, final float z) {
		this.x += x;
		this.y += y;
		this.z += z;

		return this;
	}

	public DDSVec sub(final DDSVec v) {
		x -= v.x;
		y -= v.y;
		z -= v.z;

		return this;
	}

	public DDSVec mul(final float s) {
		x *= s;
		y *= s;
		z *= s;

		return this;
	}

	public DDSVec div(final float s) {
		final float t = 1.0f / s;

		x *= t;
		y *= t;
		z *= t;

		return this;
	}

	public float dot(final DDSVec v) {
		return x * v.x + y * v.y + z * v.z;
	}
}
