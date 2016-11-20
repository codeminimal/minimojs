package br.com.kodeless.minimojs;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public enum XFileUtil {

    instance;

    private final String[] IMG_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif"};

    public final byte[] pixel;

    XFileUtil() {
        InputStream in = XFileUtil.class.getResourceAsStream("/px.gif");
        try {
            pixel = IOUtils.toByteArray(in);
        } catch (IOException e) {
            throw new RuntimeException("Erro carregando a imagem vazia!", e);
        }

    }

    public boolean validateImage(String fileName) {

        for (String extension : IMG_EXTENSIONS) {
            if (fileName.toUpperCase().endsWith(extension.toUpperCase()))
                return true;
        }

        return false;
    }

    public String getResource(String path) throws IOException {
        return XStreamUtil.inputStreamToString(XFileUtil.class.getResourceAsStream(path));
    }

    public String readFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }

    public byte[] readFromDisk(String path, String defaultPath) throws IOException {
        path = path.replaceAll("//", "/");
        String diskPath = X.getRealPath(path);
        if (diskPath != null) {
            InputStream is;
            File file = new File(diskPath);
            if (file.exists()) {
                is = new FileInputStream(file);
            } else {
                is = X.getResource(path);
                if (is == null && defaultPath != null) {
                    is = XFileUtil.class.getResourceAsStream(defaultPath);
                }
            }
            return is != null ? XStreamUtil.inputStreamToByteArray(is) : null;
        }
        return null;
    }

    public List<File> listFiles(String folderPath, FilenameFilter filter) throws IOException {
        List<File> result = new ArrayList<File>();
        folderPath = folderPath.replaceAll("//", "/");
        String diskPath = X.getRealPath(folderPath);
        if (diskPath != null) {
            File folder = new File(diskPath);
            findFiles(folder, filter, result);
        }
        return result;
    }

    private void findFiles(File folder, FilenameFilter filter, List<File> result) {
        File[] files = folder.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                findFiles(file, filter, result);
            } else if (filter.accept(folder, file.getName())) {
                result.add(file);
            }
        }
    }

    public void writeFile(String filePath, byte[] bytes) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();
        OutputStream out = new FileOutputStream(file);
        out.write(bytes);
        out.flush();
        out.close();
    }
}
