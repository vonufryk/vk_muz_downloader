import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Vitalii Onufryk
 */
public class Main {

    private static String userId = "";
    private static String songsNum = "";
    private static String login = "";
    private static String pass = "";
    public static String downloadFolderPath = "";

    public static void main(String[] args) throws Exception {

        String configFile = "config.txt";
        try {
        if (!args[0].isEmpty()) {
            songsNum = args[0];
            print("Will try to find " + songsNum + " songs.");
        }} catch(ArrayIndexOutOfBoundsException e) {
            // just do nothing
        }

        checkConfigFileExist(configFile);

        BufferedReader reader = new BufferedReader(new FileReader(configFile));
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.contains("Login")) {
                login = line.replaceAll("Login: ", "");
            }
            if (line.contains("Pass")) {
                pass = line.replaceAll("Pass: ", "");
            }
            if (line.contains("Download")) {
                downloadFolderPath = line.replaceAll("Download folder: ", "");
            }
        }

        if (login.isEmpty() || pass.isEmpty()) {
            print("Hey, you specified some login or pass wrong. Please check config.txt in this folder.");
            quit();
        }

        if (downloadFolderPath.isEmpty()) {
            downloadFolderPath = System.getProperty("user.dir") + File.separator + "vk_music_files";
            print("You didn't specified the Download folder, so I'll put the music here: " + downloadFolderPath);
        }

        print("Ok, let's start. Please do not close the FireFox, it'll be closed automatically :)");
        WebDriver driver = new FirefoxDriver();
        WebDriverWait wait = new WebDriverWait(driver, 10);
        driver.get("https://vk.com/dev/audio.get");
        (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.name("email")));
        driver.findElement(By.name("email")).sendKeys(login);
        driver.findElement(By.id("quick_pass")).sendKeys(pass);
        driver.findElement(By.id("quick_login_button")).click();
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("myprofile")));
        } catch (org.openqa.selenium.TimeoutException e) {
            if (driver.findElement(By.id("pass")).isDisplayed()) {
                print("Looks like you specified wrong login or password. Please check config.txt file.");
                driver.quit();
                quit();
            }
        }

        (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated(By.id("dev_const_owner_id")));

        // Get User ID
        String audiosHref = driver.findElement(By.xpath("(//*[starts-with(@href,'/audios')])[1]")).getAttribute("href");
        userId = getUserId(audiosHref);

        WebElement userIdInput = driver.findElement(By.id("dev_const_owner_id"));
        userIdInput.clear();
        userIdInput.sendKeys(userId);
        WebElement songsNumInput = driver.findElement(By.id("dev_const_count"));
        songsNumInput.clear();
        songsNumInput.sendKeys(songsNum);
        driver.findElement(By.id("dev_req_run_btn")).click();
        Thread.sleep(5000);

        //String audio_count = driver.findElement(By.xpath("//span[@class='dev_result_key' and text()='count:']/../span[@class='dev_result_num']")).getText();
        List<WebElement> results = driver.findElements(By.xpath("//span[@class='dev_result_lbracket']/..//div[@class='dev_result_obj']"));
        results.remove(0);
        //print("You have: " + audio_count + " audios in VK.");
        print("I've found " + results.size() + " songs.");
        print("I'll download them for you... Meanwhile you can do whatever you want :P");
        print("P.S. But please do not close FireFox, I still need it.");

        createDownloadFolder(downloadFolderPath);
        ExecutorService pool = Executors.newFixedThreadPool(10);

        List<Song> songs = new ArrayList<Song>();
        for (WebElement result : results) {
            List<WebElement> keys = result.findElements(By.cssSelector(".dev_result_key"));
            List<WebElement> values = result.findElements(By.xpath("*[@class='dev_result_str' or @class='dev_result_num']"));
            Song song = new Song();
            for (int i=0; i<keys.size(); i++) {
                String key = keys.get(i).getText();
                String value = values.get(i).getText();
                // set song fields
                if (key.equals("url:")) {
                    song.url = values.get(i).findElement(By.cssSelector("a")).getAttribute("href");
                } else if (key.equals("title:")) {
                    song.title = cleanString(value);
                } else if (key.equals("artist:")) {
                    song.artist = cleanString(value);
                }
            }
            song.name = song.artist + " - " + song.title;
            song.name = song.name.replace(File.separator, "");
            songs.add(song);
            pool.submit(new DownloadTask(song.url, song.name, songs.size() + "/" + results.size()));
        }
        driver.quit();
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        //createDownloadFolder(downloadFolderPath);
        //downloadSongs(songs);

        print("Yahoo, everything is done.");
        print("Please enjoy!");
    }

    private static String cleanString(String value) {
        return value.replaceAll("'", "").replaceAll("&#9835;", "");
    }

    private static void checkConfigFileExist(String configFile) {
        if (!(new File(configFile).exists())) {
            print("I don't see the " + configFile + " Please check it.");
            quit();
        }
    }

    private static void createDownloadFolder(String downloadFolderPath) {
        File download_folder = new File(downloadFolderPath);
        if (!download_folder.exists()) {
            download_folder.mkdirs();
        }
    }

    /**
    private static void downloadSongs(List<Song> songs) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        for (Song song : songs) {
            pool.submit(new DownloadTask(song.url, song.name));
        }
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }
    **/

    private static String getUserId(String audiosHref) {
        String pattern = "(\\d+)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(audiosHref);
        if (m.find()) {
            return m.group(0);
        } else {
            System.out.println("Couldn't get the User ID, please restart the app.");
            quit();
        }
        return "";
    }

    private static void quit() {
        System.exit(1);
    }

    private static void print(String out) {
        System.out.println(out);
    }

}
