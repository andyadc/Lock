package org.menagerie;

import java.lang.annotation.*;

/**
 * Used to indicate that a particular feature is still under heavy development and should
 * <i>not</i> be used in production.
 * <p>
 * Using a class marked with this annotation in production and having trouble is should not be considered
 * unexpected. Use these classes <i>AT YOUR OWN RISK</i>
 *
 * @author Scott Fines
 * @version 1.0
 *          Date: 11-Jan-2011
 *          Time: 21:02:12
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Beta {
}
