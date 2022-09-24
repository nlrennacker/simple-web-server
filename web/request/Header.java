package web.request;

/**
 * Accepted headers for http requests
 */
public enum Header {
   
    AIM,
    ACCEPT,
    ACCEPT_CHARSET,
    ACCEPT_DATETIME,
    ACCEPT_ENCODING,
    ACCEPT_CONTROL_REQUEST_METHOD,
    AUTHORIZATION,
    CACHE_CONTROL,
    CONNECTION,
    CONTENT_ENCODING,
    CONTENT_LENGTH,
    CONTENT_MD5,
    CONTENT_TYPE,
    COOKIE,
    DATE,
    EXPECT,
    FORWARDED,
    FROM,
    HOST,
    HTTP2_SETTINGS,
    IF_MATCH,
    IF_MODIFIED_SINCE,
    IF_NONE_MATCH,
    IF_RANGE,
    IF_UNMODIFIED_SINCE,
    MAX_FORWARDS,
    ORIGIN,
    PRAGMA,
    PREFER,
    PROXY_AUTHORIZATION,
    RANGE,
    REFERER,
    TE,
    TRAILER,
    TRANSFER_ENCODING,
    USER_AGENT,
    UPGRADE,
    VIA,
    WARNING;

    /**
     * Returns a boolean value based on whether supplied string matches any preset recognized http header
     * String should be all upper case with only alphabet characters to match the respective enums
     * @param toMatch a string to match with any respective enum
     * @return boolean value based on whether enum match was found
     */
    public static boolean contains(String toMatch){
        for(Header header: Header.values())
             if (header.name().equals(toMatch))
                return true;
        return false;
      } 
}
