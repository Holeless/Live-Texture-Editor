package com.zeeesea.textureeditor.editor;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExternalEditorDetector {
   private static List<ExternalEditorDetector.DetectedEditor> cachedEditors = null;

   public static List<ExternalEditorDetector.DetectedEditor> detectEditors() {
      if (cachedEditors != null) {
         return cachedEditors;
      }

      cachedEditors = new ArrayList();
      detectMSPaint();
      String[] programDirs = new String[]{System.getenv("ProgramFiles"), System.getenv("ProgramFiles(x86)"), System.getenv("LOCALAPPDATA")};

      for (String dir : programDirs) {
         if (dir != null) {
            Path paintNet = Path.of(dir, new String[]{"paint.net", "PaintDotNet.exe"});
            if (Files.exists(paintNet, new LinkOption[0])) {
               cachedEditors.add(new ExternalEditorDetector.DetectedEditor("Paint.NET", paintNet.toString()));
               break;
            }
         }
      }

      for (String dir : programDirs) {
         if (dir != null) {
            Path aseprite = Path.of(dir, new String[]{"Aseprite", "Aseprite.exe"});
            if (Files.exists(aseprite, new LinkOption[0])) {
               cachedEditors.add(new ExternalEditorDetector.DetectedEditor("Aseprite", aseprite.toString()));
               break;
            }
         }
      }

      Path steamAseprite = Path.of(System.getProperty("user.home"), new String[]{"AppData", "Roaming", "Aseprite", "Aseprite.exe"});
      if (Files.exists(steamAseprite, new LinkOption[0]) && cachedEditors.stream().noneMatch(e -> e.name().equals("Aseprite"))) {
         cachedEditors.add(new ExternalEditorDetector.DetectedEditor("Aseprite", steamAseprite.toString()));
      }

      for (String dir : programDirs) {
         if (dir != null) {
            Path steamAse = Path.of(dir, new String[]{"Steam", "steamapps", "common", "Aseprite", "Aseprite.exe"});
            if (Files.exists(steamAse, new LinkOption[0]) && cachedEditors.stream().noneMatch(e -> e.name().equals("Aseprite"))) {
               cachedEditors.add(new ExternalEditorDetector.DetectedEditor("Aseprite", steamAse.toString()));
               break;
            }
         }
      }

      for (String dir : programDirs) {
         if (dir != null) {
            File adobeDir = new File(dir, "Adobe");
            if (adobeDir.isDirectory()) {
               File[] children = adobeDir.listFiles();
               if (children != null) {
                  for (File child : children) {
                     if (child.isDirectory() && child.getName().contains("Photoshop")) {
                        File ps = new File(child, "Photoshop.exe");
                        if (ps.exists()) {
                           cachedEditors.add(new ExternalEditorDetector.DetectedEditor("Photoshop", ps.getAbsolutePath()));
                           break;
                        }
                     }
                  }
               }
            }
         }
      }

      for (String dir : programDirs) {
         if (dir != null) {
            File gimpBase = new File(dir);
            File[] children = gimpBase.listFiles();
            if (children != null) {
               for (File child : children) {
                  if (child.isDirectory() && child.getName().toLowerCase().contains("gimp")) {
                     File gimp = new File(child, "bin" + File.separator + "gimp-2.10.exe");
                     if (gimp.exists()) {
                        cachedEditors.add(new ExternalEditorDetector.DetectedEditor("GIMP", gimp.getAbsolutePath()));
                        break;
                     }

                     gimp = new File(child, "bin" + File.separator + "gimp.exe");
                     if (gimp.exists()) {
                        cachedEditors.add(new ExternalEditorDetector.DetectedEditor("GIMP", gimp.getAbsolutePath()));
                        break;
                     }
                  }
               }
            }
         }
      }

      return cachedEditors;
   }

   private static void detectMSPaint() {
      String system32 = System.getenv("SystemRoot");
      if (system32 != null) {
         Path mspaint = Path.of(system32, new String[]{"system32", "mspaint.exe"});
         if (Files.exists(mspaint, new LinkOption[0])) {
            cachedEditors.add(new ExternalEditorDetector.DetectedEditor("MS Paint", mspaint.toString()));
            return;
         }
      }

      try {
         ProcessBuilder pb = new ProcessBuilder(new String[]{"where", "mspaint"});
         pb.redirectErrorStream(true);
         Process proc = pb.start();
         BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
         String line = reader.readLine();
         proc.waitFor();
         if (line != null && !line.isEmpty() && new File(line.trim()).exists()) {
            cachedEditors.add(new ExternalEditorDetector.DetectedEditor("MS Paint", line.trim()));
            return;
         }
      } catch (Exception var10) {
      }

      try {
         ProcessBuilder pb = new ProcessBuilder(new String[]{"where", "mspaint.exe"});
         pb.redirectErrorStream(true);
         Process proc = pb.start();
         BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
         String line = reader.readLine();
         proc.waitFor();
         if (line != null && !line.isEmpty() && new File(line.trim()).exists()) {
            cachedEditors.add(new ExternalEditorDetector.DetectedEditor("MS Paint", line.trim()));
            return;
         }
      } catch (Exception var9) {
      }

      try {
         ProcessBuilder pb = new ProcessBuilder(new String[]{"cmd", "/c", "where", "mspaint"});
         pb.redirectErrorStream(true);
         Process proc = pb.start();
         BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
         String line = reader.readLine();
         int exitCode = proc.waitFor();
         if (exitCode == 0 && line != null && !line.isEmpty()) {
            cachedEditors.add(new ExternalEditorDetector.DetectedEditor("MS Paint", "mspaint"));
            return;
         }
      } catch (Exception var8) {
      }

      String localAppData = System.getenv("LOCALAPPDATA");
      if (localAppData != null) {
         File windowsApps = new File(localAppData, "Microsoft" + File.separator + "WindowsApps");
         if (windowsApps.isDirectory()) {
            File[] files = windowsApps.listFiles();
            if (files != null) {
               for (File f : files) {
                  if (f.getName().toLowerCase().contains("mspaint") && f.getName().endsWith(".exe")) {
                     cachedEditors.add(new ExternalEditorDetector.DetectedEditor("MS Paint", f.getAbsolutePath()));
                     return;
                  }
               }
            }
         }
      }
   }

   public static void clearCache() {
      cachedEditors = null;
   }

   public static ExternalEditorDetector.DetectedEditor getDefault() {
      List<ExternalEditorDetector.DetectedEditor> editors = detectEditors();
      return editors.isEmpty() ? null : (ExternalEditorDetector.DetectedEditor)editors.get(0);
   }

   public record DetectedEditor(String name, String executablePath) {
   }
}
