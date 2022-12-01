import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.IIOException;

import javax.imageio.ImageIO;

public class TestImageFilter {

	public static void main(String[] args) throws Exception {
		
		BufferedImage image = null;
		String srcFileName = null;
		try {
			srcFileName = args[0];
			File srcFile = new File(srcFileName);
			image = ImageIO.read(srcFile);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Usage: java TestAll <image-file>");
			System.exit(1);
		}
		catch (IIOException e) {
			System.out.println("Error reading image file " + srcFileName + " !");
			System.exit(1);
		}

		System.out.println("Source image: " + srcFileName);

		int w = image.getWidth();
		int h = image.getHeight();
		System.out.println("Image size is " + w + "x" + h);
		System.out.println();
	
		int[] src = image.getRGB(0, 0, w, h, null, 0, w);
		int[] dst = new int[src.length];

		System.out.println("Starting sequential image filter.");

		long startTime = System.currentTimeMillis();
		ImageFilter filter0 = new ImageFilter(src, dst, w, h);
		filter0.apply();
		long endTime = System.currentTimeMillis();

		long tSequential = endTime - startTime; 
		System.out.println("Sequential image filter took " + tSequential + " milliseconds.");

		BufferedImage dstImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		dstImage.setRGB(0, 0, w, h, dst, 0, w);

		String dstName = "Filtered" + srcFileName;
		File dstFile = new File(dstName);
		ImageIO.write(dstImage, "jpg", dstFile);

		System.out.println("Output image: " + dstName);	

		var sequentialDst = dst.clone();

		System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());

		// Compare parallell image filter for 2, 4, 8, 16, 32 threads
		for (int nthreads = 1; nthreads <= 32; nthreads *= 2) {
			System.out.println();
			System.out.println("Starting parallell image filter using " + nthreads + " threads.");

			src = image.getRGB(0, 0, w, h, null, 0, w);
			dst = new int[src.length];

			startTime = System.currentTimeMillis();
			ParallelFJImageFilter filter = new ParallelFJImageFilter(src, dst, w, h);
			filter.apply(nthreads);
			endTime = System.currentTimeMillis();

			long tParallel = endTime - startTime; 
			System.out.println("Parallel image filter took " + tParallel + " milliseconds using " + nthreads + " threads.");

			boolean isValid = true;
			// compare that the results are the same with sequential
			for (int i = 0; i < dst.length; i++) {
				if (dst[i] != sequentialDst[i]) {
					isValid = false;
					break;
				}
			}

			if (isValid) {
			  System.out.println("Output image verified successfully.");
			} else {
			  System.out.println("Output image is not valid!");
			}

			double expectedMinimumSpeedup = nthreads * 0.7;
			double speedup = (double)tSequential / tParallel;
			boolean isSpeedupEnough = speedup >= expectedMinimumSpeedup;
			System.out.println("Speedup: " + speedup + " " + (isSpeedupEnough ? "ok (>= " : "not ok (< ") + expectedMinimumSpeedup + ")");
		}
		
		BufferedImage parallelDstImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		parallelDstImage.setRGB(0, 0, w, h, dst, 0, w);

		String parallelDstName = "ParallelFiltered" + srcFileName;
		File parallelDstFile = new File(parallelDstName);
		ImageIO.write(parallelDstImage, "jpg", parallelDstFile);

		System.out.println("Output image: " + parallelDstName);	
	}
}
