package com.http.servlets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import com.http.helpers.Logger;
import com.http.helpers.OpenCVHelper;
import com.utils.TimeService;

@MultipartConfig
@WebServlet(urlPatterns = { "/upload/*" }, asyncSupported = true, loadOnStartup = 1)
public class UploadServlet extends HttpServlet {
	private static boolean DEBUG_WATERMARK = false;
	private static HashMap<Integer, Integer> indeces = new HashMap<Integer, Integer>();

	private static String imageText = "AutorazborkaBY*AutorazborkaBY*AutorazborkaBY*AutorazborkaBY";

	private static final long serialVersionUID = 1L;

	public UploadServlet() {
		super();
		Logger.print("Upload servlet starting...");

		OpenCVHelper.initialize();

		Logger.print("Upload servlet initialized");
	}

	public void destroy() {

	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String urls = request.getParameter("urls");
		String noDownloadUrls = request.getParameter("no_download_urls");

		if (noDownloadUrls != null) {
			processImageWithNoDowload(noDownloadUrls);
		}

		if (urls == null) {
			System.err.println("No urls found in request");
			return;
		}

		TimeService.submit(() -> {
			JSONArray arr = new JSONArray(urls);
			Logger.print("Got " + arr.length() + " images to download and process");
			int l = arr.length();

			for (int i = 0; i < l; i++) {
				JSONObject obj = arr.optJSONObject(i);

				if (obj == null) {
					continue;
				}

				String url = obj.optString("url");
				String dest = obj.optString("dest");
				int index = obj.optInt("index", -1);
				int maxIndex = obj.optInt("max_index", -1);
				int productId = obj.optInt("product_id", -1);

				if (productId > 0) {
					int current = indeces.getOrDefault(productId, 0);
					indeces.put(productId, current);
				}

				if (!url.isEmpty() && !dest.isEmpty()) {
					TimeService.submit(() -> {
						boolean errorOccured = false;
						URL u = null;
						HttpURLConnection huc = null;
						ReadableByteChannel readableByteChannel = null;
						FileOutputStream fileOutputStream = null;
						FileChannel fileChannel = null;

						try {
							Logger.print("Downloading file from " + url + " to " + dest);
							u = new URL(url);
							huc = (HttpURLConnection) u.openConnection();
							huc.setConnectTimeout(5000);
							int responseCode = 0;

							try {
								responseCode = huc.getResponseCode();
							} catch (Exception $hucEx) {
								Logger.print("Image " + url + " connection error, retrying...");

								try {
									huc = (HttpURLConnection) u.openConnection();
									huc.setConnectTimeout(5000);
									responseCode = huc.getResponseCode();
								} catch (Exception $hucEx2) {
									// keep silence
								}
							}

							if (responseCode != HttpURLConnection.HTTP_OK) {
								Logger.print("Image " + url + " does not exist, skipping...");
								return;
							}

							readableByteChannel = Channels.newChannel(u.openStream());
							fileOutputStream = new FileOutputStream(dest);
							fileChannel = fileOutputStream.getChannel();
							fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

							Logger.print("File " + dest + " downloaded successfully");
						} catch (Exception ex1) {
							ex1.printStackTrace();
							errorOccured = true;
						} finally {
							if (fileChannel != null) {
								try {
									fileChannel.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}

							if (fileOutputStream != null) {
								try {
									fileOutputStream.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}

							if (readableByteChannel != null) {
								try {
									readableByteChannel.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}

							if (huc != null) {
								huc.disconnect();
							}
						}

						if (!errorOccured) {
							try {
								File input = new File(dest);

								if (!input.isFile()) {
									Logger.print("Input file " + dest + " does not exist. Skipping...");
									return;
								}

								if (!input.setReadable(true, false)) {
									Logger.print("Unable to set read rights to file " + dest);
								}

								if (!input.setWritable(true, false)) {
									Logger.print("Unable to set write rights to file " + dest);
								}

								if (!input.setExecutable(true, false)) {
									Logger.print("Unable to set execute rights to file " + dest);
								}

								if (url.contains("bamper.by")) {
									removeWatermark(input, dest);
									Logger.print("Removed watermark from " + dest);
								} else {
									addImageText(input, dest);
									Logger.print("Added text to " + dest);
								}
							} catch (Exception ex1) {
								ex1.printStackTrace();
							} finally {
								if (productId > 0) {
									int current = indeces.getOrDefault(productId, 0);
									indeces.put(productId, current + 1);
								}

								setProductsProcessed(index, maxIndex, productId);
							}
						} else {
							if (productId > 0) {
								int current = indeces.getOrDefault(productId, 0);
								indeces.put(productId, current + 1);
							}

							setProductsProcessed(index, maxIndex, productId);
						}
					});
				}
			}

			Logger.print("Finished processing array of images");
		});
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	private void addImageText(File input, String destination) {
		try {
			Mat file = Imgcodecs.imread(input.getAbsolutePath());
			Mat result = new Mat();
			file.copyTo(result);
			Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2BGRA);
			Size size = result.size();

			Mat text = new Mat(result.height() * 2, result.width() * 2, CvType.CV_8UC3, colorRGB(0, 0, 0));
			Imgproc.cvtColor(text, text, Imgproc.COLOR_BGR2BGRA);

			Imgproc.putText(text, imageText, new Point(0, size.height * 1.29), Imgproc.FONT_HERSHEY_COMPLEX, 1,
					colorRGB(255, 255, 255), 3, Imgproc.LINE_AA, false);

			Mat rotatedText = new Mat();
			Mat matrix = Imgproc.getRotationMatrix2D(new Point(result.width(), result.height()), 35, 1);

			Imgproc.warpAffine(text, rotatedText, matrix, new Size(text.width(), text.height()), Imgproc.INTER_LINEAR,
					Core.BORDER_TRANSPARENT, new Scalar(0, 0, 0, 255));

			text.release();
			matrix.release();
			Rect rectCrop = new Rect((rotatedText.width() - result.width()) / 2, (rotatedText.height() - result.height()) / 2,
					result.width(), result.height());
			Mat roi = new Mat(rotatedText, rectCrop);
			rotatedText.release();

			Core.addWeighted(result, 1, roi, 0.25, 0, result);
			roi.release();

			Imgcodecs.imwrite(destination, result);
			result.release();
			file.release();

			removeOldImages(input);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static Mat getCropped(Mat source) {
		Mat grayscale = new Mat();
		Mat thresh = new Mat();
		Mat points = new Mat();

		Imgproc.cvtColor(source, grayscale, Imgproc.COLOR_BGR2GRAY);
		Imgproc.threshold(grayscale, thresh, 236, 255, Imgproc.THRESH_BINARY_INV);
		Core.findNonZero(thresh, points);
		Rect rect = Imgproc.boundingRect(points);

		if (DEBUG_WATERMARK) {
			Logger.print(rect.toString());
		}

		Mat result = new Mat(source, rect);
		grayscale.release();
		thresh.release();
		points.release();
		return result;
	}

	private static Mat getCropped(Mat source, int x, int y) {
		Mat grayscale = new Mat();
		Mat thresh = new Mat();
		Mat points = new Mat();

		Imgproc.cvtColor(source, grayscale, Imgproc.COLOR_BGR2GRAY);
		Imgproc.threshold(grayscale, thresh, 236, 255, Imgproc.THRESH_BINARY_INV);
		Core.findNonZero(thresh, points);

		Rect rect = new Rect(0, 0, x, y);

		if (DEBUG_WATERMARK) {
			Logger.print(rect.toString());
		}

		Mat result = new Mat(source, rect);
		grayscale.release();
		thresh.release();
		points.release();
		return result;
	}

	private void processImageWithNoDowload(String urls) {
		JSONArray arr = new JSONArray(urls);
		Logger.print("Got " + arr.length() + " images to process");
		int l = arr.length();

		for (int i = 0; i < l; i++) {
			try {
				JSONObject obj = arr.optJSONObject(i);

				if (obj == null) {
					continue;
				}

				String path = obj.optString("path");
				File input = new File(path);

				if (!input.isFile()) {
					Logger.print("Input file " + path + " does not exist. Skipping...");
					return;
				}

				if (!input.setReadable(true, false)) {
					Logger.print("Unable to set read rights to file " + path);
				}

				if (!input.setWritable(true, false)) {
					Logger.print("Unable to set write rights to file " + path);
				}

				if (!input.setExecutable(true, false)) {
					Logger.print("Unable to set execute rights to file " + path);
				}

				addImageText(input, path);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private void removeWatermark(File input, String destination) {
		Mat file = Imgcodecs.imread(input.getAbsolutePath());
		Mat cropped = getCropped(file);

		final int width = cropped.width();
		final int height = cropped.height();
		final int offsetX = 640 - width;
		final int offsetY = 480 - height;

		Mat img = new Mat();
		Core.copyMakeBorder(cropped, img, 0, offsetY, 0, offsetX, Core.BORDER_DEFAULT, new Scalar(0, 255, 0));
		cropped.release();

		Size size = img.size();

		if (DEBUG_WATERMARK) {
			Logger.print("Got image of size " + size.width + "x" + size.height);
		}

		Mat copy = new Mat();
		img.copyTo(copy);

		double tan = 0.7;
		double x1 = 187;
		double a1 = size.width - x1;
		double y1 = size.height - a1 * tan;
		double offset = 38;
		double x2 = x1 + offset;
		double a2 = size.width - x2;
		double y2 = size.height - a2 * tan;

		MatOfPoint pointsTop = new MatOfPoint(new Point(0, 0), new Point(size.width, 0), new Point(size.width, y1),
				new Point(x1, size.height), new Point(0, size.height));

		Imgproc.fillConvexPoly(copy, pointsTop, new Scalar(255, 255, 255));

		MatOfPoint pointsBottom = new MatOfPoint(new Point(x2, size.height), new Point(size.width, y2),
				new Point(size.width, size.height));

		Imgproc.fillConvexPoly(copy, pointsBottom, new Scalar(255, 255, 255));

		Mat grayscale = new Mat();
		Imgproc.cvtColor(copy, grayscale, Imgproc.COLOR_BGR2GRAY);
		copy.release();

		Scalar mean = Core.mean(grayscale);
		double m = mean.val[0];

		if (DEBUG_WATERMARK) {
			Logger.print("mean is " + m);
		}

		Mat thresh = new Mat();
		Imgproc.threshold(grayscale, thresh, m, 255, Imgproc.THRESH_TRUNC);
		Mat kernel = Mat.ones(5, 5, CvType.CV_8UC1);

		Mat morph = new Mat();
		Mat morph1 = new Mat();
		Imgproc.morphologyEx(thresh, morph, Imgproc.MORPH_CLOSE, kernel);
		Imgproc.morphologyEx(morph, morph1, Imgproc.MORPH_OPEN, kernel);
		kernel.release();

		kernel = Mat.ones(3, 3, CvType.CV_8UC1);
		Imgproc.morphologyEx(morph1, morph, Imgproc.MORPH_OPEN, kernel);

		morph1.release();
		kernel.release();
		grayscale.release();

		Mat subtract = new Mat();
		Core.subtract(morph, thresh, subtract);
		morph.release();
		thresh.release();

		Mat threshPre = new Mat();
		Imgproc.threshold(subtract, threshPre, 10, 255, Imgproc.THRESH_BINARY);
		subtract.release();

		Imgproc.fillConvexPoly(threshPre, pointsTop, new Scalar(255, 255, 255));
		pointsTop.release();
		Imgproc.fillConvexPoly(threshPre, pointsBottom, new Scalar(255, 255, 255));
		pointsBottom.release();

		Mat invert = new Mat();
		Core.bitwise_not(threshPre, invert);
		threshPre.release();
		kernel.release();

		kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
		Mat dilate = new Mat();
		Point kp = new Point(-1.0, -1.0);
		Imgproc.dilate(invert, dilate, kernel, kp, 2);

		invert.release();
		Mat result = new Mat();
		Photo.inpaint(img, dilate, result, 32, Photo.INPAINT_TELEA);
		img.release();
		dilate.release();

		if (DEBUG_WATERMARK) {
			Logger.print("finished removal");
		}

		Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2BGRA);
		Mat text = new Mat(result.height() * 2, result.width() * 2, CvType.CV_8UC3, colorRGB(0, 0, 0));
		Imgproc.cvtColor(text, text, Imgproc.COLOR_BGR2BGRA);

		Imgproc.putText(text, imageText, new Point(0, size.height * 1.29), Imgproc.FONT_HERSHEY_COMPLEX, 1,
				colorRGB(255, 255, 255), 3, Imgproc.LINE_AA, false);

		Mat rotatedText = new Mat();
		Mat matrix = Imgproc.getRotationMatrix2D(new Point(result.width(), result.height()), 35, 1);

		Imgproc.warpAffine(text, rotatedText, matrix, new Size(text.width(), text.height()), Imgproc.INTER_LINEAR,
				Core.BORDER_TRANSPARENT, new Scalar(0, 0, 0, 255));

		text.release();
		matrix.release();
		Rect rectCrop = new Rect((rotatedText.width() - result.width()) / 2, (rotatedText.height() - result.height()) / 2,
				result.width(), result.height());
		Mat roi = new Mat(rotatedText, rectCrop);
		rotatedText.release();

		Core.addWeighted(result, 1, roi, 0.25, 0, result);
		roi.release();
		cropped = getCropped(result, width, height);
		result.release();
		Imgcodecs.imwrite(destination, cropped);
		cropped.release();
		file.release();

		removeOldImages(input);

		if (DEBUG_WATERMARK) {
			Logger.print("finished text add");
		}
	}

	private static void removeOldImages(File input) {
		String fileName = input.getParent();
		fileName = fileName.replace(File.separator + "products", File.separator + "tmp" + File.separator + "products");
		File tmpFolder = new File(fileName);

		if (tmpFolder.exists() && tmpFolder.isDirectory()) {
			int dotIndex = input.getName().lastIndexOf('.');
			String match = (dotIndex == -1) ? input.getName() : input.getName().substring(0, dotIndex);

			if (DEBUG_WATERMARK) {
				Logger.print("Deleting files from temporary folder...");
				Logger.print("Searching matches for " + match);
			}

			final File[] files = tmpFolder.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(final File dir, final String name) {
					return name.indexOf(match) >= 0;
				}
			});

			for (final File tempFile : files) {
				if (!tempFile.delete()) {
					System.err.println("Can't remove temp file " + tempFile.getAbsolutePath());
				} else {
					System.err.println("Removed temp file " + tempFile.getAbsolutePath());
				}
			}
		}
	}

	private static void setProductsProcessed(int index, int maxIndex, int productId) {
		if (index >= 0 && maxIndex >= 0 && productId >= 0) {
			int current = indeces.getOrDefault(productId, 0);
			Logger.print(index + " " + current + " " + maxIndex);

			if (index == maxIndex - 1 || current == maxIndex) {
				JSONArray idArr = new JSONArray();
				idArr.put(productId);
				
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("data", idArr.toString());
				HashMap<String, String> response = ScheduleServlet.sendApiRequest("productprocessed", data);

				if (response.get("status").equals("200")) {
					Logger.print("Product " + productId + " set processed successfully");
				} else {
					System.err.println("Error setting product " + productId + " processed: " + response.get("message"));
				}

				indeces.remove(productId);
			}
		}

	}

	public static Scalar colorRGB(double red, double green, double blue) {
		return new Scalar(blue, green, red);
	}
}
