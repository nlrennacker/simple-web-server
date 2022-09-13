package web.request;

public enum Header {
    AIM,
    ACCEPT,
    ACCEPTCHARSET,
    ACCEPTDATETIME,
    ACCEPTENCODING,
    ACCEPTCONTROLREQUESTMETHOD,
    AUTHORIZATION,
    CACHECONTROL,
    CONNECTION,
    CONTENTENCODING,
    CONTENTLENGTH,
    CONTENTMD5,
    CONTENTTYPE,
    COOKIE,
    DATE,
    EXPECT,
    FORWARDED,
    FROM,
    HOST,
    HTTP2SETTINGS,
    IFMATCH,
    IFMODIFIEDSINCE,
    IFNONEMATCHED,
    IFRANGE,
    IFUNMODIFIEDSINCE,
    MAXFORWARDS,
    ORIGIN,
    PRAGMA,
    PREFER,
    PROXYAUTHORIZATION,
    RANGE,
    REFERER,
    TE,
    TRAILER,
    TRANSFERENCODING,
    USERAGENT,
    UPGRADE,
    VIA,
    WARNING;

    public static boolean contains(String s){
        for(Header header: Header.values())
             if (header.name().equals(s)) 
                return true;
        return false;
      } 
}
