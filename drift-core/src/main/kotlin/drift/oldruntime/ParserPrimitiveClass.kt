/******************************************************************************
 * Drift Programming Language                                                 *
 *                                                                            *
 * Copyright (c) 2026. Jonathan (GitHub: belicfr)                             *
 *                                                                            *
 * This source code is licensed under the MIT License.                        *
 * See the LICENSE file in the root directory for details.                    *
 ******************************************************************************/
package drift.oldruntime

enum class ParserPrimitiveClass(val className: String) {

    Int("Int"),
    Int64("Int64"),
    UInt("UInt"),

    String("String"),

    Bool("Bool"),

    Array("Array"),
}