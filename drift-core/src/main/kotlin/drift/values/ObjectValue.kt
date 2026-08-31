/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.values

import drift.types.ObjectType
import drift.types.ParserType
import language.QualifiedName


interface ObjectValue : Value {

    val qualifiedName: QualifiedName


    @Deprecated("To delete with old interpreter")
    override fun type(): ParserType = ObjectType(qualifiedName)
}