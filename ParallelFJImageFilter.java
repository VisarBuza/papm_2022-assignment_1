import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Iterative nine-point image convolution filter working on linearized image.
 * In each of the NRSTEPS iteration steps, the average RGB-value of each pixel
 * in the source array is computed taking into account the pixel and its 8
 * neighbor
 * pixels (in 2D) and written to the destination array.
 * The computation is done in parallel using the Java Fork/Join framework.
 */
public class ParallelFJImageFilter {
    private int[] src;
    private int[] dst;
    private int width;
    private int height;

    private final int NRSTEPS = 100;

    public ParallelFJImageFilter(int[] src, int[] dst, int w, int h) {
        this.src = src;
        this.dst = dst;
        width = w;
        height = h;
    }

    public void apply(int nthreads) {
        try (
            // create a ForkJoinPool with nthreads threads
            ForkJoinPool pool = new ForkJoinPool(nthreads)) {
            for (int i = 0; i < NRSTEPS; i++) {
                ImageFilterTask task = new ImageFilterTask(1, height - 2);
                // start the task
                pool.invoke(task);

                // swap references
                int[] help = src;
                src = dst;
                dst = help;
            }
        }
    }

    public class ImageFilterTask extends RecursiveAction {
        private int start;
        private int end;

        public ImageFilterTask(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public void compute() {
            if (end - start >= 1) {
                // split the task
                int mid = (start + end) / 2;
                ImageFilterTask left = new ImageFilterTask(start, mid);
                ImageFilterTask right = new ImageFilterTask(mid + 1, end);
                // invoke the tasks
                left.fork();
                right.compute();
                left.join();
                return;
            }

            runKernel();
        }

        private void runKernel() {
            // do the work
            int index, pixel;
            // every row
            for (int i = start; i <= end; i++) {
                // every col
                for (int j = 1; j < width - 1; j++) {
                    float rt = 0, gt = 0, bt = 0;
                    // for every neighbor up and down
                    for (int k = i - 1; k <= i + 1; k++) {
                        // left pixel
                        index = k * width + j - 1;
                        pixel = src[index];
                        rt += (float) ((pixel & 0x00ff0000) >> 16);
                        gt += (float) ((pixel & 0x0000ff00) >> 8);
                        bt += (float) ((pixel & 0x000000ff));

                        // middle pixel
                        index = k * width + j;
                        pixel = src[index];
                        rt += (float) ((pixel & 0x00ff0000) >> 16);
                        gt += (float) ((pixel & 0x0000ff00) >> 8);
                        bt += (float) ((pixel & 0x000000ff));

                        // right pixel
                        index = k * width + j + 1;
                        pixel = src[index];
                        rt += (float) ((pixel & 0x00ff0000) >> 16);
                        gt += (float) ((pixel & 0x0000ff00) >> 8);
                        bt += (float) ((pixel & 0x000000ff));
                    }
                    // Re-assemble destination pixel.
                    index = i * width + j;
                    int dpixel = (0xff000000) | (((int) rt / 9) << 16) | (((int) gt / 9) << 8) | (((int) bt / 9));
                    dst[index] = dpixel;
                }
            }
        }
    }
}