package com.majeur.ars;

import com.majeur.ars.Utils.OS;
import com.majeur.ars.Utils.OS.OSName;
import com.majeur.ars.adb.AdbHelper;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;

public class AdbRemoteScreen implements Runnable {

	private static String[] ARGS;

	public static void main(String... args) {
		ARGS = args;
		EventQueue.invokeLater(new AdbRemoteScreen());
	}

	@Override
	public void run() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		String customAdbPath = ARGS.length == 0 ? LocalProperties.adbPath : ARGS[0];
		File adbExecutable = loadCustomAdbExecutable(customAdbPath);

		if (adbExecutable == null) {
			adbExecutable = loadInternalAdbExecutable();
			if (adbExecutable == null)
				return;
		}

		final File adbExecutableFinal = adbExecutable;
		new MainFrame(new AdbHelper(adbExecutableFinal));
	}

	private File loadCustomAdbExecutable(String path) {
		if (path == null)
			return null;

		File adbExecutable = new File(path);
		if (adbExecutable.exists()) {
			Logger.i("Adb custom path used: %s", adbExecutable.getPath());
			return adbExecutable;
		} else {
			Logger.w("No valid adb executable found at \"%s\". Falling back to default internal executable.", path);
			return null;
		}
	}

	private File loadInternalAdbExecutable() {
		URL adbExecutableURL = null;
		switch (OS.name) {
			case WINDOWS:
				adbExecutableURL = getClass().getResource("/adb_2701_win");
				break;
			case LINUX:
				adbExecutableURL = getClass().getResource("/adb_2701_linux");
				break;
			case MAC:
				JOptionPane.showMessageDialog(null,
						"Adb Remote Screen does not support MAC OS yet.", "Error",
						JOptionPane.ERROR_MESSAGE);
				return null;
		}

		File tempDirectory = new File(Utils.getRunningJarPath() + File.separator + "Temp");
		if (!tempDirectory.exists())
			tempDirectory.mkdirs();
		tempDirectory.deleteOnExit();

		final File tempAdbExecutable = new File(tempDirectory, "adb");
		tempAdbExecutable.deleteOnExit();
		try {
			if (!tempAdbExecutable.exists())
				tempAdbExecutable.createNewFile();
			createTempExecutable(adbExecutableURL, tempAdbExecutable);
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}

		Logger.i("Adb executable loaded for %s into %s", OS.name, tempAdbExecutable.getPath());

		return tempAdbExecutable;
	}

	private void createTempExecutable(URL adbSourceExecutableURL, File destFile) throws IOException {
		InputStream is = adbSourceExecutableURL.openStream();
		OutputStream os = new FileOutputStream(destFile);

		byte[] b = new byte[2048];
		int length;
		while ((length = is.read(b)) != -1) {
			os.write(b, 0, length);
		}

		is.close();
		os.close();

		if (OS.name == OSName.LINUX)
			destFile.setExecutable(true, false);// TODO Fix this
	}
}
