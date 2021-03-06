package io.takari.jdkget.osx;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.codehaus.plexus.util.FileUtils;

import com.sprylab.xar.XarEntry;
import com.sprylab.xar.XarFile;

import io.takari.jdkget.IJdkExtractor;
import io.takari.jdkget.IOutput;
import io.takari.jdkget.JdkGetter.JdkVersion;

public class OsxJDKExtractor implements IJdkExtractor {

  @Override
  public boolean extractJdk(JdkVersion jdkVersion, File jdkDmg, File outputDirectory, File inProcessDirectory, IOutput output) throws IOException {
    
    output.info("Extracting osx dmg image into " + outputDirectory);
    
    // DMG <-- XAR <-- GZ <-- CPIO
    try {
      UnHFS.unhfs(jdkDmg, inProcessDirectory);
    } catch (Exception e) {
      output.error(e.getMessage());
      return false;
    }

    List<File> files = FileUtils.getFiles(inProcessDirectory, "**/*.pkg", null, true);
    // validate
    
    File jdkPkg = files.get(0);
    XarFile xarFile = new XarFile(jdkPkg);
    for (XarEntry entry : xarFile.getEntries()) {
      if (!entry.isDirectory() && entry.getName().startsWith("jdk") && entry.getName().endsWith("Payload")) {
        File file = new File(inProcessDirectory, entry.getName());
        file.getParentFile().mkdirs();
        try (InputStream is = entry.getInputStream(); OutputStream os = new FileOutputStream(file)) {
          IOUtils.copy(is, os);
        }
      }
    }

    files = FileUtils.getFiles(inProcessDirectory, "**/Payload", null, true);
    File jdkGz = files.get(0);
    File cpio = new File(inProcessDirectory, "temp" + System.currentTimeMillis() + ".cpio");
    try (GZIPInputStream is = new GZIPInputStream(new FileInputStream(jdkGz)); FileOutputStream os = new FileOutputStream(cpio)) {
      IOUtils.copy(is, os);
    }

    // https://people.freebsd.org/~kientzle/libarchive/man/cpio.5.txt
    try (ArchiveInputStream is = new CpioArchiveInputStream(new FileInputStream(cpio))) {
      CpioArchiveEntry e;
      while ((e = (CpioArchiveEntry) is.getNextEntry()) != null) {
        if (!e.isDirectory()) {
          File jdkFile = new File(outputDirectory, e.getName());
          jdkFile.getParentFile().mkdirs();
          if (e.isRegularFile()) {
            try (OutputStream os = new FileOutputStream(jdkFile)) {
              IOUtils.copy(is, os);
            }
          } else if (e.isSymbolicLink()) {
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
              IOUtils.copy(is, os);
              String target = new String(os.toByteArray());
              if(File.pathSeparatorChar == ';') {
                output.info("Not creating symbolic link " + e.getName() + " -> " + target);
              } else {

                Files.createSymbolicLink(jdkFile.toPath(), Paths.get(target));
              }
            }
          }
          // The lower 9 bits specify read/write/execute permissions for world, group, and user following standard POSIX conventions.
          if(File.pathSeparatorChar != ';') {
            int mode = (int) e.getMode() & 0000777;
            Files.setPosixFilePermissions(jdkFile.toPath(), PosixModes.intModeToPosix(mode));
          }
        }
      }
    }
    
    return true;
  }

}
