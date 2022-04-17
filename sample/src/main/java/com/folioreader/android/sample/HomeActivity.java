/*
 * Copyright (C) 2016 Pedro Paulo de Amorim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.folioreader.android.sample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.folioreader.Config;
import com.folioreader.Constants;
import com.folioreader.FolioReader;
import com.folioreader.model.HighLight;
import com.folioreader.model.locators.ReadLocator;
import com.folioreader.ui.base.OnSaveHighlight;
import com.folioreader.ui.fragment.FolioContentFragment;
import com.folioreader.util.AppUtil;
import com.folioreader.util.OnHighlightListener;
import com.folioreader.util.ReadLocatorListener;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HomeActivity extends AppCompatActivity
        implements OnHighlightListener, ReadLocatorListener, FolioReader.OnClosedListener {

    private static final String LOG_TAG = HomeActivity.class.getSimpleName();
    private FolioReader folioReader;


    private final String TEST_URL = "https://api.fictionate.me/chapters/A6EB4BEC-4963-4579-BC0E-BB0227EC1714/epub.epub?AuthToken=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ1dWlkIjoiQUExN0JBOTYtNDJERi00NTI1LTlGMzgtNDNCRUNERDVBMDU3In0.euEQkcVKfOqW-5PXOqFTbwTBnqGmh8oB8TNvKBDeCPVv-QovnDn-r_VHTFyQ0kwDSjkEr77mSZt8nPuNsGk8K-whsqWk5nVgifxYDStYFDctS97BqYXOVYgQf6tra0ivTEMpxDs5iaJwzbPsfHPWsAGdSeLZ1yJMclR8N5hLEkwQ3maqOl_Q3zp472w_tWFE7d2JYfWxG6UdzFA64bXgXxhM5kgUXBG7jXIhf_SPsH2FFIb90mIenE-PFGB8JhkKZyQy2Ma0ibr1PcB-uS6TD2sJHdOHGNzpz0ypvzMTI0ny7CBLM969uofQEKA89bPsjhuu7j5m7rUngXAdCFpArF_EDHyUCJFNXV-dfAKPo941TohaTODt0F0hk_SxhkB6Fkwc6RthzR9x11GQ1to7xDw-2BKmFwEo2GmZEm1Z9ezcvJDpgsRII8trFN6JnHGK8V4zglLI3xKBeRkkwcKvPlIchQ0Vq2G-Pff3EB33nphEqUtih7uvKriapT0OXNQtNMXxig0TF5cIu7JuJy-00kofuGFnJrjelnvOrHhGJYtZhpJrCKqZXWWJ-i5_AHAkfnfPRIIR2SSXIggTQtZR-56PDYU-PFLQFtm-ArDfa2Ohiq-XSbcjXtx4iCb_82rl1L2C3IAfbKv6MsjGOHtIVaCROdEqW2gjt5ZoTJ8RIXs";
    private final String META_DATA = "\t<dc:title>Sample</dc:title>\n" +
            "\t<dc:creator>Author</dc:creator>\n" +
            "\t<dc:language>es</dc:language>\n" +
            "\t<dc:rights>Rights</dc:rights>\n" +
            "\t<dc:publisher>Publisher</dc:publisher>\n" +
            "\t<dc:identifier id=\"isbn\">0123456789012</dc:identifier>\n";
    private File BOOKS_DIRECTORY;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        folioReader = FolioReader.get()
                .setOnHighlightListener(this)
                .setReadLocatorListener(this)
                .setOnClosedListener(this);

        ReadLocator readLocator = getLastReadLocator();

        Config config = new Config()
                .setAllowedDirection(Config.AllowedDirection.ONLY_HORIZONTAL)
                .setDirection(Config.Direction.HORIZONTAL)
                .setFont(Config.Font.GEORGIA)
                .setColorMode(Config.ColorMode.YELLOW)
                .setIgnoreFirstChapter(true)
                .setFontSize(6);

//        Config config = AppUtil.getSavedConfig(getApplicationContext());
//        if (config == null)
//            config = new Config();
//        config.setAllowedDirection(Config.AllowedDirection.VERTICAL_AND_HORIZONTAL);

        folioReader.setReadLocator(readLocator);
        folioReader.setConfig(config, true);

        BOOKS_DIRECTORY = getApplicationContext().getFilesDir();

//        openBook("file:///android_asset/epub2.epub");

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(TEST_URL).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response){
                if (!response.isSuccessful()) {
//                    throw new IOException("Unexpected code " + response);
                } else {
                    String fileName = extractId(TEST_URL) + ".epub";
                    if (!isPresent(fileName)) {
                        try (ResponseBody body = response.body()) {
                            saveBook(fileName, Objects.requireNonNull(body).bytes());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                    }
                    openBook(BOOKS_DIRECTORY + "/" + fileName);
                }
            }
        });



//        getHighlightsAndSave();

//        findViewById(R.id.btn_raw).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                Config config = AppUtil.getSavedConfig(getApplicationContext());
//                if (config == null)
//                    config = new Config();
//                config.setAllowedDirection(Config.AllowedDirection.VERTICAL_AND_HORIZONTAL);
//
//                folioReader.setConfig(config, true)
//                        .openBook(R.raw.accessible_epub_3);
//            }
//        });

//        findViewById(R.id.btn_assest).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                ReadLocator readLocator = getLastReadLocator();
//
//                Config config = AppUtil.getSavedConfig(getApplicationContext());
//                if (config == null)
//                    config = new Config();
//                config.setAllowedDirection(Config.AllowedDirection.VERTICAL_AND_HORIZONTAL);
//
//                folioReader.setReadLocator(readLocator);
//                folioReader.setConfig(config, true);
//                openBook("file:///android_asset/TheSilverChair.epub");
//            }
//        });
    }

    public void openBook(String path) {
        Bundle bundle = folioReader.getBundleFromUrl(path);
        Fragment folioReaderFragment = new FolioContentFragment();
        folioReaderFragment.setArguments(bundle);
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.contentFrame, folioReaderFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private boolean isPresent(String fileName) {
        File[] directoryListing = BOOKS_DIRECTORY.listFiles();
        if (directoryListing != null) {
            for (File file : directoryListing) {
                String _fileName = file.getName();
                if (_fileName.equals(fileName)) {
                    return true;
                }
            }
        } else {
            // Handle the case where dir is not really a directory.
            // Checking dir.isDirectory() above would not be sufficient
            // to avoid race conditions with another process that deletes
            // directories.

        }
        return false;
    }

    private void saveBook(String fileName, byte[] bytes) throws IOException {
        File epub = new File(BOOKS_DIRECTORY + "/", fileName);
        FileUtils.writeByteArrayToFile(epub, Objects.requireNonNull(bytes));
        new ZipFile(epub).extractFile("content.opf", String.valueOf(BOOKS_DIRECTORY));
        File opf = new File(BOOKS_DIRECTORY + "/", "content.opf");
        addMetadata(opf);
        new ZipFile(epub).addFile(opf);
        opf.delete();
    }

    private String extractId(String url) {
        Pattern pattern = Pattern.compile("chapters/(.+?)/epub");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private void addMetadata(File opfFile) throws IOException {
        StringBuilder fileData = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(opfFile));
            String line;

            while ((line = br.readLine()) != null) {
                if(line.contains("</metadata>")) {
                    fileData.append(META_DATA);
                }
                fileData.append(line);
                fileData.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
        FileWriter writer = new FileWriter(opfFile, false);
        writer.write(fileData.toString());
        writer.close();
    }

//    private boolean getFile(String url) {
//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder().url(url).build();
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                e.printStackTrace();
//            }
//
//            @Override
//            public void onResponse(Call call, final Response response) throws IOException {
//                if (!response.isSuccessful()) {
//                    throw new IOException("Unexpected code " + response);
//                } else {
//                    FileUtils.writeByteArrayToFile(new File(getApplicationContext().getFilesDir(), "tempFile.epub"),
//                            Objects.requireNonNull(response.body()).bytes());
//                }
//            }
//        });
//        try {
//            Response response = client.newCall(request).execute();
//            if (response.isSuccessful()) {
//                FileUtils.writeByteArrayToFile(new File(getApplicationContext().getFilesDir(), "tempFile.epub"),
//                        Objects.requireNonNull(response.body()).bytes());
//
//                return true;
//            } else {
//                throw new IOException("Unexpected code " + response);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

    private ReadLocator getLastReadLocator() {

        String jsonString = loadAssetTextAsString("Locators/LastReadLocators/last_read_locator_1.json");
        return ReadLocator.fromJson(jsonString);
    }

    @Override
    public void saveReadLocator(ReadLocator readLocator) {
        Log.i(LOG_TAG, "-> saveReadLocator -> " + readLocator.toJson());
    }

    /*
     * For testing purpose, we are getting dummy highlights from asset. But you can get highlights from your server
     * On success, you can save highlights to FolioReader DB.
     */
    private void getHighlightsAndSave() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<HighLight> highlightList = null;
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    highlightList = objectMapper.readValue(
                            loadAssetTextAsString("highlights/highlights_data.json"),
                            new TypeReference<List<HighlightData>>() {
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (highlightList == null) {
                    folioReader.saveReceivedHighLights(highlightList, new OnSaveHighlight() {
                        @Override
                        public void onFinished() {
                            //You can do anything on successful saving highlight list
                        }
                    });
                }
            }
        }).start();
    }

    private String loadAssetTextAsString(String name) {
        BufferedReader in = null;
        try {
            StringBuilder buf = new StringBuilder();
            InputStream is = getAssets().open(name);
            in = new BufferedReader(new InputStreamReader(is));

            String str;
            boolean isFirst = true;
            while ((str = in.readLine()) != null) {
                if (isFirst)
                    isFirst = false;
                else
                    buf.append('\n');
                buf.append(str);
            }
            return buf.toString();
        } catch (IOException e) {
            Log.e("HomeActivity", "Error opening asset " + name);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e("HomeActivity", "Error closing asset " + name);
                }
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FolioReader.clear();
    }

    @Override
    public void onHighlight(HighLight highlight, HighLight.HighLightAction type) {
        Toast.makeText(this,
                "highlight id = " + highlight.getUUID() + " type = " + type,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFolioReaderClosed() {
        Log.v(LOG_TAG, "-> onFolioReaderClosed");
    }
}