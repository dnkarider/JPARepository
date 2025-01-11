package org.example.diplomaServer.service;

import org.example.diplomaServer.model.AuthTokenInfo;
import org.example.diplomaServer.repository.FileStorageRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class FileStorageService {
    private final FileStorageRepository personRepository;
    private final String uploadDir = "uploads/";


    public FileStorageService(FileStorageRepository personRepository) {
        this.personRepository = personRepository;
    }

    public AuthTokenInfo getAuth(String login, String password){
        return personRepository.getAuth(login, password);
    }

    public String deleteAuthToken(String auth_token) throws IOException {
        return personRepository.deleteAuthToken(auth_token);
    }

    public String saveFile(MultipartFile file, String auth_token) throws IOException {
        if(!personRepository.findSimilarAuthToken(auth_token)){
            throw new IOException("auth-token does not exist");
        }
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File absoluteFile = new File(dir.getAbsolutePath());
        File destinationFile = new File(absoluteFile, file.getOriginalFilename());
        file.transferTo(destinationFile);
        return destinationFile.getAbsolutePath();
    }

    public File getFile(String fileName, String auth_token) throws IOException {
        if (!personRepository.findSimilarAuthToken(auth_token)) {
            throw new IOException("auth-token does not exist");
        }
        return new File(uploadDir, fileName);
    }

    public File deleteFile(String fileName, String auth_token) throws IOException {
        if (!personRepository.findSimilarAuthToken(auth_token)) {
            throw new IOException("auth-token does not exist");
        }
        return new File(uploadDir, fileName);
    }

    public String updateFile(String auth_token) throws IOException {
        if (!personRepository.findSimilarAuthToken(auth_token)) {
            throw new IOException("auth-token does not exist");
        }
        return uploadDir;
    }

    public File[] getList(String auth_token) throws IOException {
        if (!personRepository.findSimilarAuthToken(auth_token)) {
            throw new IOException("auth-token does not exist");
        }
        File directory = new File(uploadDir);
        File[] files = directory.listFiles();
        return files;
    }
    public boolean fileExists(File file) {
        return file.exists();
    }

    public boolean renameFile(File oldFile, File newFile) {
        return oldFile.renameTo(newFile);
    }
}
