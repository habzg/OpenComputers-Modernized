package li.cil.oc.api.fs;

/**
 * Represents a handle to a file opened from a {@link FileSystem}.
 */
public interface Handle {
    /**
     * The current position in the file.
     */
    long position();

    /**
     * The total length of the file.
     */
    long length();

    /**
     * Closes the handle.
     * <br>
     * For example, if there is an underlying stream, this should close that
     * stream. Any future calls to {@link #read} or {@link #write} should throw
     * an <code>IOException</code> after this function was called.
     */
    void close();

    /**
     * Tries to read as much data from the file as fits into the specified
     * array.
     * <br>
     * For files opened in write or append mode this should always throw an
     * exception.
     *
     * @param into the buffer to read the data into.
     * @return the number of bytes read; -1 if there are no more bytes (EOF).
     */
    int read(byte[] into);

    /**
     * Jump to the specified position in the file, if possible.
     * <br>
     * For files opened in write or append mode this should always throw an
     * exception.
     *
     * @param to the position in the file to jump to.
     * @return the resulting position in the file.
     */
    long seek(long to);

    /**
     * Tries to write all the data from the specified array into the file.
     * <br>
     * For files opened in read mode this should always throw an exception.
     *
     * @param value the data to write into the file.
     */
    void write(byte[] value);
}
