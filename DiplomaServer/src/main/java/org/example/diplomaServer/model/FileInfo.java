package org.example.diplomaServer.model;

public class FileInfo {
    private String filename;
    private long size;

    public FileInfo(String filename, long size) {
        this.filename = filename;
        this.size = size;
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }
}