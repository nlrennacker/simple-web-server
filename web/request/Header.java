package web.request;

/**
 * Accepted headers for http requests
 */
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

    /**
     * Returns a boolean value based on whether supplied string matches any preset recognized http header
     * String should be all upper case with only alphabet characters to match the respective enums
     * @param a string to match with any respective enum
     * @return boolean value based on whether enum match was found
     */
    public static boolean contains(String s){
        for(Header header: Header.values())
             if (header.name().equals(s)) 
                return true;
        return false;
      } 
}
