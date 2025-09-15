package it.yuruni.kariview.client.sound;


public class FFT {
    private final int n;
    private final int log2n;

    public FFT(int n) {
        this.n = n;
        this.log2n = (int) (Math.log(n) / Math.log(2));
    }

    public void transform(float[] data) {
        // Corrected check: FFT expects an array of size 2 * n for complex data.
        if (data.length != 2 * n) {
            throw new IllegalArgumentException("Data length must be " + (2 * n));
        }

        // Bit-reversal permutation
        int j = 0;
        for (int i = 0; i < n; i++) {
            if (i < j) {
                // Swap real parts
                float tempReal = data[i * 2];
                data[i * 2] = data[j * 2];
                data[j * 2] = tempReal;

                // Swap imaginary parts
                float tempImag = data[i * 2 + 1];
                data[i * 2 + 1] = data[j * 2 + 1];
                data[j * 2 + 1] = tempImag;
            }
            int m = n / 2;
            while (j >= m && m > 0) {
                j -= m;
                m /= 2;
            }
            j += m;
        }

        // Radix-2 FFT
        for (int len = 2; len <= n; len <<= 1) {
            float angle = (float) (-2 * Math.PI / len);
            float w_r = (float) Math.cos(angle);
            float w_i = (float) Math.sin(angle);

            for (int i = 0; i < n; i += len) {
                float u_r = 1.0f;
                float u_i = 0.0f;

                for (int k = 0; k < len / 2; k++) {
                    int i0 = 2 * (i + k);           // base index
                    int i1 = 2 * (i + k + len / 2); // paired index

                    float a_r = data[i0];
                    float a_i = data[i0 + 1];
                    float b_r = data[i1];
                    float b_i = data[i1 + 1];

                    // Apply twiddle factor
                    float t_r = u_r * b_r - u_i * b_i;
                    float t_i = u_r * b_i + u_i * b_r;

                    // Butterfly
                    data[i0]     = a_r + t_r;
                    data[i0 + 1] = a_i + t_i;
                    data[i1]     = a_r - t_r;
                    data[i1 + 1] = a_i - t_i;

                    // Update twiddle
                    float next_u_r = u_r * w_r - u_i * w_i;
                    float next_u_i = u_r * w_i + u_i * w_r;
                    u_r = next_u_r;
                    u_i = next_u_i;
                }
            }
        }

    }
}