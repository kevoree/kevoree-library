package org.kevoree.library.cloud.api.helper;

import org.kevoree.TypeDefinition;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 16/01/14
 * Time: 10:50
 *
 * @author Erwan Daubert
 * @version 1.0
 */
public class CloudModelHelper {
    public static boolean isASubType(TypeDefinition typeDefinition, String typeName) {
        if (!typeName.equals(typeDefinition.getName())) {
            boolean isSubType = false;
            for (TypeDefinition superType : typeDefinition.getSuperTypes()) {
                if (isASubType(superType, typeName)) {
                    isSubType = true;
                    break;
                }
            }
            return isSubType;
        } else {
            return true;
        }
    }
}
