package web.resource;

import web.server.configuration.HttpdConf;
import web.server.configuration.MimeTypes;
import web.server.configuration.utils.ConfigurationReader;


public final class ConfigResource {

    private static ConfigResource instance;
    private static HttpdConf httpdConf;
    private static MimeTypes mimeTypes;
    private static final Integer DEFAULT_PORT = 8080;

    private ConfigResource() {}

    // static block initialization for exception handling
    static {
        try {
            httpdConf = new HttpdConf(ConfigurationReader.readConfiguration("conf/httpd.conf"));
            mimeTypes = new MimeTypes(ConfigurationReader.readConfiguration("conf/mime.types"));
            instance = new ConfigResource();
        } catch (Exception e) {
            System.out.println("Configuration files not found...");
            e.printStackTrace();
        }
    }

    public static ConfigResource getInstance() {
        return instance;
    }

    public static HttpdConf getHttpdConf() {
        return httpdConf;
    }

    public static MimeTypes getMimeTypes() {
        return mimeTypes;
    }

    public static Integer getDefaultPort(){
        return DEFAULT_PORT;
    }

}
