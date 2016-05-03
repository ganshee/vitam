package fr.gouv.vitam.api.exception;

public class MetaDataExecutionException  extends MetaDataException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8131926677545592877L;

	/**
     * @param message
     *            message to associate with the exception
     */
    public MetaDataExecutionException(String message) {
        super(message);
    }

    /**
     * @param cause
     *            cause to associate with the exception
     */
    public MetaDataExecutionException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     *            message to associate with the exception
     * @param cause
     *            cause to associate with the exception
     */
    public MetaDataExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}