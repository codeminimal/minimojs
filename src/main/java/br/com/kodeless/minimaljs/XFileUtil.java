package br.com.kodeless.minimaljs;

import org.apache.commons.io.IOUtils;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public enum XFileUtil {

    instance;

    private final String[] IMG_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif"};

    private byte[] gif;

    XFileUtil() {
        InputStream in = XFileUtil.class.getResourceAsStream("/px.gif");
        try {
            gif = IOUtils.toByteArray(in);
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

    public void printUploadResponse(boolean ok) throws IOException {

        OutputStream out = XContext.getXResponse().getOutputStream();
        out.write(("<html><script>parent.X._uploadResponse('" + ok + "');</script></html>").getBytes());
        out.flush();
    }

    public String getResource(String path) throws IOException {
        return XStreamUtil.inputStreamToString(XServlet.class.getResourceAsStream(path));
    }

    public void printEmptyGif() throws IOException {
        XContext.getXResponse().setContentType("image/gif");
        OutputStream out = XContext.getXResponse().getOutputStream();
        out.write(gif);
        out.flush();
    }

    public void sendFile(XFile f) throws IOException {
        XContext.getXResponse().setContentType(f.getContentType());
        XContext.getXResponse().setContentLength(f.getData().length);
        OutputStream output = XContext.getXResponse().getOutputStream();
        output.write(f.getData());
        output.flush();
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

    public byte[] readFromDisk(String path, String defaultPath, ServletContext ctx) throws IOException {
        path = path.replaceAll("//", "/");
        String diskPath = ctx.getRealPath(path);
        if (diskPath != null) {
            InputStream is;
            File file = new File(diskPath);
            if (file.exists()) {
                is = new FileInputStream(file);
            } else {
                is = ctx.getResourceAsStream(path);
                if (is == null && defaultPath != null) {
                    is = XServlet.class.getResourceAsStream(defaultPath);
                }
            }
            return is != null ? XStreamUtil.inputStreamToByteArray(is) : null;
        }
        return null;
    }

    public List<File> listFiles(String folderPath, FilenameFilter filter, ServletContext ctx) throws IOException {
        List<File> result = new ArrayList<File>();
        folderPath = folderPath.replaceAll("//", "/");
        String diskPath = ctx.getRealPath(folderPath);
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
}
