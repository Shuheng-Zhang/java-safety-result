package top.shuz.java.safety.result;

/**
 * @author heng
 * @since 2025/12/19
 */
public class ResultException extends RuntimeException {

    final public static String ERROR_NULL = "[Error] cannot be NULL";

    final public static String VALUE_BUT_ERROR = "Cannot unwrap an error result";
    final public static String ERROR_BUT_NULL = "Cannot call unwrapError() on a success result";

    public ResultException(final String msg) {
        super(msg);
    }
}
