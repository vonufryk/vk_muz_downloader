import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class DownloadTask implements Runnable {

    private final String url;
    private final String name;
    private final String info;

    public DownloadTask(String url, String name, String info) {
        this.url = url;
        this.name = name;
        this.info = info;
    }

    @Override
    public void run() {
        String error_message = "Couldn't download file " + name + ".mp3";
        try {
            downloadFile(url, name);
        } catch (MalformedURLException e) {
            System.out.println(error_message);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println(error_message);
            e.printStackTrace();
        }
    }

    private void downloadFile(String url, String name) throws IOException {
        String filePath = Main.downloadFolderPath + File.separator + name + ".mp3";
        if (!(new File(filePath).exists())) {
            System.out.println(info + ": " + name);
            URL website = new URL(url);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } else {
            System.out.println("File '" + name + ".mp3" + "' is already present.");
        }

    }
}