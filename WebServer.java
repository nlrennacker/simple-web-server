import web.server.configuration.MimeTypes;
import web.server.configuration.utils.ConfigurationReader;
import web.server.configuration.HttpdConf;

import java.util.Map;

public class WebServer {
    public static void main(String[] args) {
        HttpdConf httpdConf = new HttpdConf(ConfigurationReader.readConfiguration("conf/httpd.conf"));
        MimeTypes mimeTypes = new MimeTypes(ConfigurationReader.readConfiguration("conf/mime.types"));

        // Test code
        httpdConf.getListen().ifPresentOrElse(
            opt -> System.out.printf("httpd listen: %s%n", opt),
            () -> System.out.println("httpd listen: NOT CONFIGURED"));
        httpdConf.getDocumentRoot().ifPresentOrElse(
            opt -> System.out.printf("httpd document root: %s%n", opt),
            () -> System.out.println("httpd document root: NOT CONFIGURED"));
        httpdConf.getLogFile().ifPresentOrElse(
            opt -> System.out.printf("httpd log file: %s%n", opt),
            () -> System.out.println("httpd log file: NOT CONFIGURED"));
        httpdConf.getAliases().ifPresentOrElse(
            opt -> {
                for (Map.Entry<String, String> entry : opt.entrySet()) {
                    System.out.printf("httpd aliases: %s -> %s%n", entry.getKey(), entry.getValue());
                }
            },
            () -> System.out.println("httpd aliases: NOT CONFIGURED"));
        httpdConf.getScriptAliases().ifPresentOrElse(
            opt -> {
                for (Map.Entry<String, String> entry : opt.entrySet()) {
                    System.out.printf("httpd script aliases: %s -> %s%n", entry.getKey(), entry.getValue());
                }
            },
            () -> System.out.println("httpd script aliases: NOT CONFIGURED"));
        httpdConf.getAccessFile().ifPresentOrElse(
            opt -> System.out.printf("httpd access file: %s%n", opt),
            () -> System.out.println("httpd access file: NOT CONFIGURED"));
        httpdConf.getDirectoryIndexes().ifPresentOrElse(
            opt -> {
                for (String str : opt) {
                    System.out.printf("httpd directory indexes: %s%n", str);
                }
            },
            () -> System.out.println("httpd directory indexes: NOT CONFIGURED"));
        for (Map.Entry<String, String> entry : mimeTypes.getMimeTypes().entrySet()) {
            System.out.printf("mime type mapping: %s %s%n", entry.getKey(), entry.getValue());
        }
    }
}
