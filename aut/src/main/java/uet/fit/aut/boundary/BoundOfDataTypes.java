package uet.fit.aut.boundary;

//public class BoundOfDataTypes {
//    public DataSizeModel bounds = new DataSizeModel();
//
//    private static long TWO_POWER_7 = 127;
//    private static long TWO_POWER_8 = 255;
//
//    private static long TWO_POWER_15 = 32767l;
//    private static long TWO_POWER_16 = 65535l;
//
//    private static long TWO_POWER_31 = 2147483647l;
//    private static long TWO_POWER_32 = 4294967295l;
//
//    private static long TWO_POWER_63 = 9223372036854775807l;
//    private static double TWO_POWER_64 = 18446744073709551615f;//9223372036854775807l * 2;
//
//    private static long MAX_UNSIGNED_CHAR_32 = 1114111; // 0x10ffff
//
////    /**
////     * Ref: https://en.cppreference.com/w/cpp/language/types
////     *
////     * @return
////     */
////    public DataSizeModel createLP32() {
////        DataSizeModel bounds = new DataSizeModel();
////
////        // Character types
////        bounds.put("char", new PrimitiveBound(0, TWO_POWER_8));
////        bounds.put("char8_t", new PrimitiveBound(0, TWO_POWER_8));
////        bounds.put("char16_t", new PrimitiveBound(0, TWO_POWER_16));
////        bounds.put("char32_t", new PrimitiveBound(0, MAX_UNSIGNED_CHAR_32)); // need to confirm
////        bounds.put("wchar_t", new PrimitiveBound(0, TWO_POWER_32));
////        bounds.put("signed char", new PrimitiveBound(-TWO_POWER_7 - 1, TWO_POWER_7));
////
////        // Integer types (signed)
////        bounds.put("short", new PrimitiveBound(-TWO_POWER_15 - 1, TWO_POWER_15));
////        bounds.put("short int", bounds.get("short"));
////        bounds.put("signed short int", bounds.get("short"));
////        bounds.put("signed short", bounds.get("short"));
////
////        bounds.put("int", new PrimitiveBound(-TWO_POWER_15, TWO_POWER_15));
////        bounds.put("signed", bounds.get("int"));
////        bounds.put("signed int", bounds.get("int"));
////
////        bounds.put("long", new PrimitiveBound(-TWO_POWER_31, TWO_POWER_31));
////        bounds.put("long int", bounds.get("long"));
////        bounds.put("signed long", bounds.get("long"));
////        bounds.put("signed long int", bounds.get("long"));
////
////        bounds.put("long long", new PrimitiveBound(-TWO_POWER_63, TWO_POWER_63));
////        bounds.put("long long int", bounds.get("long long"));
////        bounds.put("signed long long", bounds.get("long long"));
////        bounds.put("signed long long int", bounds.get("long long"));
////
////        // Integer types (unsigned)
////        bounds.put("unsigned char", new PrimitiveBound(0, TWO_POWER_8));//
////        bounds.put("unsigned short", new PrimitiveBound(0, TWO_POWER_16));//
////        bounds.put("unsigned short int", new PrimitiveBound(0, TWO_POWER_16));//
////        bounds.put("unsigned int", new PrimitiveBound(0, TWO_POWER_16));//
////        bounds.put("unsigned long int", new PrimitiveBound(0, TWO_POWER_32));//
////        bounds.put("unsigned", new PrimitiveBound(0, TWO_POWER_16));
////        bounds.put("unsigned long long int", new PrimitiveBound(0 + "", TWO_POWER_64 + ""));//
////        bounds.put("unsigned long", new PrimitiveBound(0, TWO_POWER_32));//
////        bounds.put("unsigned long long", new PrimitiveBound(0 + "", TWO_POWER_64 + ""));//
////
////        // Floating-point types
////        bounds.put("float", new PrimitiveBound(-TWO_POWER_31, TWO_POWER_31));
////        bounds.put("double", new PrimitiveBound(-TWO_POWER_63, TWO_POWER_63));
////        bounds.put("long double", new PrimitiveBound(-TWO_POWER_63, TWO_POWER_63));
////
////        // Boolean type
////        bounds.put("bool", new PrimitiveBound(0, 1));
////
////        return bounds;
////    }
//    /**
//     * Ref: https://en.cppreference.com/w/cpp/language/types
//     *
//     * @return
//     */
//    public DataSizeModel createLP64() {
//        DataSizeModel bounds = new DataSizeModel();
//
//        // Character types
//        bounds.put("char", new PrimitiveBound(0, TWO_POWER_8));
//        bounds.put("char8_t", new PrimitiveBound(0, TWO_POWER_8));
//        bounds.put("char16_t", new PrimitiveBound(0, TWO_POWER_16));
//        bounds.put("char32_t", new PrimitiveBound(0, MAX_UNSIGNED_CHAR_32)); // need to confirm
//        bounds.put("wchar_t", new PrimitiveBound(0, TWO_POWER_32));
//        bounds.put("signed char", new PrimitiveBound(-TWO_POWER_7 - 1, TWO_POWER_7));
//        bounds.put("unsigned char", new PrimitiveBound(0, TWO_POWER_8));//
//
//        // Integer types 16 bits
//        bounds.put("short", new PrimitiveBound(-TWO_POWER_15 - 1, TWO_POWER_15));
//        bounds.put("short int", bounds.get("short"));
//        bounds.put("signed short", bounds.get("short"));
//        bounds.put("signed short int", bounds.get("short"));
//        bounds.put("unsigned short", new PrimitiveBound(0, TWO_POWER_16));//
//        bounds.put("unsigned short int", new PrimitiveBound(0, TWO_POWER_16));//
//
//        // LP64: 32 bits
//        bounds.put("int", new PrimitiveBound(-TWO_POWER_31 - 1, TWO_POWER_31));
//        bounds.put("signed", bounds.get("int"));
//        bounds.put("signed int", bounds.get("int"));
//        bounds.put("unsigned", new PrimitiveBound(0, TWO_POWER_32));
//        bounds.put("unsigned int", new PrimitiveBound(0, TWO_POWER_32));//
//
//        // LP64: 64 bits
//        bounds.put("long", new PrimitiveBound(-TWO_POWER_63 - 1, TWO_POWER_63));
//        bounds.put("long int", bounds.get("long"));
//        bounds.put("signed long", bounds.get("long"));
//        bounds.put("signed long int", bounds.get("long"));
//        bounds.put("unsigned long", new PrimitiveBound(0 + "", TWO_POWER_64 + ""));//
//        bounds.put("unsigned long int", new PrimitiveBound(0 + "", TWO_POWER_64 + ""));//
//
//        // LP64: 64 bits
//        bounds.put("long long", new PrimitiveBound(-TWO_POWER_63 - 1, TWO_POWER_63));
//        bounds.put("long long int", bounds.get("long long"));
//        bounds.put("signed long long", bounds.get("long long"));
//        bounds.put("signed long long int", bounds.get("long long"));
//        bounds.put("unsigned long long", new PrimitiveBound(0 + "", TWO_POWER_64 + ""));//
//        bounds.put("unsigned long long int", new PrimitiveBound(0 + "", TWO_POWER_64 + ""));//
//
//        // Floating-point types
//        bounds.put("float", new PrimitiveBound(-TWO_POWER_31 - 1, TWO_POWER_31));
//        bounds.put("double", new PrimitiveBound(-TWO_POWER_63 - 1, TWO_POWER_63));
//        // need to be check again. 80bit instead of 64 bit
//        bounds.put("long double", new PrimitiveBound(-TWO_POWER_63 - 1, TWO_POWER_63)); // 80bit x87 on x86 and x86-64 architecture
//
//        // Boolean type
//        bounds.put("bool", new PrimitiveBound(0, 1));
//
//        addStdIntBound(bounds);
//        return bounds;
//    }
//
//    public void addStdIntBound(DataSizeModel bounds){
//        bounds.put("intmax_t", new PrimitiveBound(-TWO_POWER_31,TWO_POWER_31));
//        bounds.put("uintmax_t", new PrimitiveBound(0,TWO_POWER_32));
//        bounds.put("int8_t", new PrimitiveBound(-TWO_POWER_7,TWO_POWER_7));
//        bounds.put("uint8_t", new PrimitiveBound(0,TWO_POWER_8));
//        bounds.put("int16_t", new PrimitiveBound(-TWO_POWER_15,TWO_POWER_15));
//        bounds.put("uint16_t", new PrimitiveBound(0,TWO_POWER_16));
//        bounds.put("int32_t", new PrimitiveBound(-TWO_POWER_31,TWO_POWER_31));
//        bounds.put("uint32_t", new PrimitiveBound(0,TWO_POWER_32));
//        bounds.put("int64_t", new PrimitiveBound(-TWO_POWER_63,TWO_POWER_63));
//        bounds.put("uint64_t", new PrimitiveBound(0, (long) TWO_POWER_64));
//
//        bounds.put("int_least8_t", new PrimitiveBound(-TWO_POWER_7,TWO_POWER_7));
//        bounds.put("uint_least8_t", new PrimitiveBound(0,TWO_POWER_8));
//        bounds.put("int_least16_t", new PrimitiveBound(-TWO_POWER_15,TWO_POWER_15));
//        bounds.put("uint_least16_t", new PrimitiveBound(0,TWO_POWER_16));
//        bounds.put("int_least32_t", new PrimitiveBound(-TWO_POWER_31,TWO_POWER_31));
//        bounds.put("uint_least32_t", new PrimitiveBound(0,TWO_POWER_32));
//        bounds.put("int_least64_t", new PrimitiveBound(-TWO_POWER_63,-TWO_POWER_63));
//        bounds.put("uint_least64_t", new PrimitiveBound(0, (long) TWO_POWER_64));
//        bounds.put("int_fast8_t", new PrimitiveBound(-TWO_POWER_7,TWO_POWER_7));
//        bounds.put("uint_fast8_t", new PrimitiveBound(0,TWO_POWER_8));
//        bounds.put("int_fast16_t", new PrimitiveBound(-TWO_POWER_15,TWO_POWER_15));
//        bounds.put("uint_fast16_t", new PrimitiveBound(0,TWO_POWER_16));
//        bounds.put("int_fast32_t", new PrimitiveBound(-TWO_POWER_31,TWO_POWER_31));
//        bounds.put("uint_fast32_t", new PrimitiveBound(0,TWO_POWER_32));
//        bounds.put("int_fast64_t", new PrimitiveBound(-TWO_POWER_63,TWO_POWER_63));
//        bounds.put("uint_fast64_t", new PrimitiveBound(0, (long) TWO_POWER_64));
//    }
//    /**
//     * Ref: https://en.cppreference.com/w/cpp/language/types
//     *
//     * @return
//     */
//    public DataSizeModel createLP32() {
//        DataSizeModel bounds = new DataSizeModel();
//
//        // Character types
//        bounds.put("char", new PrimitiveBound(0, TWO_POWER_8));
//        bounds.put("char8_t", new PrimitiveBound(0, TWO_POWER_8));
//        bounds.put("char16_t", new PrimitiveBound(0, TWO_POWER_16));
//        bounds.put("char32_t", new PrimitiveBound(0, MAX_UNSIGNED_CHAR_32)); // need to confirm
//        bounds.put("wchar_t", new PrimitiveBound(0, TWO_POWER_32));
//        bounds.put("signed char", new PrimitiveBound(-TWO_POWER_7 - 1, TWO_POWER_7));
//        bounds.put("unsigned char", new PrimitiveBound(0, TWO_POWER_8));//
//
//        // Integer types
//        bounds.put("short", new PrimitiveBound(-TWO_POWER_15 - 1, TWO_POWER_15));
//        bounds.put("short int", bounds.get("short"));
//        bounds.put("signed short", bounds.get("short"));
//        bounds.put("signed short int", bounds.get("short"));
//        bounds.put("unsigned short", new PrimitiveBound(0, TWO_POWER_16));//
//        bounds.put("unsigned short int", new PrimitiveBound(0, TWO_POWER_16));//
//
//        bounds.put("int", new PrimitiveBound(-TWO_POWER_15 - 1, TWO_POWER_15)); // LP32: 16bit; LP64: 32 bit
//        bounds.put("signed", bounds.get("int"));
//        bounds.put("signed int", bounds.get("int"));
//        bounds.put("unsigned", new PrimitiveBound(0, TWO_POWER_16));
//        bounds.put("unsigned int", new PrimitiveBound(0, TWO_POWER_16));//
//
//        bounds.put("long", new PrimitiveBound(-TWO_POWER_31 - 1, TWO_POWER_31)); // LP32: 32bit; LP64: 64 bit
//        bounds.put("long int", bounds.get("long"));
//        bounds.put("signed long", bounds.get("long"));
//        bounds.put("signed long int", bounds.get("long"));
//        bounds.put("unsigned long", new PrimitiveBound(0, TWO_POWER_32));//
//        bounds.put("unsigned long int", new PrimitiveBound(0, TWO_POWER_32));//
//
//        bounds.put("long long", new PrimitiveBound(-TWO_POWER_63 - 1, TWO_POWER_63));
//        bounds.put("long long int", bounds.get("long long"));
//        bounds.put("signed long long", bounds.get("long long"));
//        bounds.put("signed long long int", bounds.get("long long"));
//        bounds.put("unsigned long long", new PrimitiveBound(0 + "", TWO_POWER_64 + ""));//
//        bounds.put("unsigned long long int", new PrimitiveBound(0 + "", TWO_POWER_64 + ""));//
//
//        // Floating-point types
//        bounds.put("float", new PrimitiveBound(-TWO_POWER_31 - 1, TWO_POWER_31));
//        bounds.put("double", new PrimitiveBound(-TWO_POWER_63 - 1, TWO_POWER_63));
//        // need to be check again. 80bit instead of 64 bit
//        bounds.put("long double", new PrimitiveBound(-TWO_POWER_63 - 1, TWO_POWER_63)); // 80bit x87 on x86 and x86-64 architecture
//
//        // Boolean type
//        bounds.put("bool", new PrimitiveBound(0, 1));
//
//        addStdIntBound(bounds);
//        return bounds;
//    }
//
//    public DataSizeModel getBounds() {
//        return bounds;
//    }
//
//    public void setBounds(DataSizeModel bounds) {
//        this.bounds = bounds;
//    }
//
//    public final static String MODEL_LP32 = "LP32";
//    public final static String MODEL_LP64 = "LP64";
//}


public class BoundOfDataTypes {

    public final static String MODEL_LLP64 = "LLP64";
    public final static String MODEL_LP32 = "LP32";
    public final static String MODEL_ILP32 = "ILP32";
    public final static String MODEL_LP64 = "LP64";

    private static final String FLOAT_MIN_VALUE = "-3.4E38";
    private static final String FLOAT_MAX_VALUE = "3.4E38";
    private static final String DOUBLE_MIN_VALUE = "-1.7E308";
    private static final String DOUBLE_MAX_VALUE = "1.7E308";
    private static final String LONG_DOUBLE_MIN_VALUE = "-3.4E4932";
    private static final String LONG_DOUBLE_MAX_VALUE = "3.4E4932";

    private DataSizeModel bounds = new DataSizeModel();

    public DataSizeModel createLP64() {
        DataSizeModel bounds = new DataSizeModel();

        // Character types
        bounds.put("char", createSignedIntBound(1));
        bounds.put("signed char", bounds.get("char"));
        bounds.put("unsigned char", createUnsignedIntBound(1));
        bounds.put("wchar_t", createUnsignedIntBound(4));
        bounds.put("char8_t", createUnsignedIntBound(1));
        bounds.put("char16_t", createUnsignedIntBound(2));
        bounds.put("char32_t", createUnsignedIntBound(4));

        // Integer types
        bounds.put("short", createSignedIntBound(2));
        bounds.put("short int", bounds.get("short"));
        bounds.put("signed short", bounds.get("short"));
        bounds.put("signed short int", bounds.get("short"));
        bounds.put("unsigned short", createUnsignedIntBound(2));
        bounds.put("unsigned short int", bounds.get("unsigned short"));

        bounds.put("int", createSignedIntBound(4));
        bounds.put("signed", bounds.get("int"));
        bounds.put("signed int", bounds.get("int"));
        bounds.put("unsigned", createUnsignedIntBound(4));
        bounds.put("unsigned int", bounds.get("unsigned"));//

        bounds.put("long", createSignedIntBound(8));
        bounds.put("long int", bounds.get("long"));
        bounds.put("signed long", bounds.get("long"));
        bounds.put("signed long int", bounds.get("long"));
        bounds.put("unsigned long", new PrimitiveBound("0", "18446744073709551615"));
        bounds.put("unsigned long int", bounds.get("unsigned long"));

        bounds.put("long long", createSignedIntBound(8));
        bounds.put("long long int", bounds.get("long long"));
        bounds.put("signed long long", bounds.get("long long"));
        bounds.put("signed long long int", bounds.get("long long"));
        bounds.put("unsigned long long", new PrimitiveBound("0", "18446744073709551615"));
        bounds.put("unsigned long long int", bounds.get("unsigned long long"));

        // Floating-point types
        bounds.put("float", new PrimitiveBound(FLOAT_MIN_VALUE, FLOAT_MAX_VALUE));
        bounds.put("double", new PrimitiveBound(DOUBLE_MIN_VALUE, DOUBLE_MAX_VALUE));
        bounds.put("long double", new PrimitiveBound(LONG_DOUBLE_MIN_VALUE, LONG_DOUBLE_MAX_VALUE)); // 80bit x87 on x86 and x86-64 architecture

        // Boolean type
        bounds.put("bool", createSignedIntBound(1));

        // Size_t
        bounds.put("size_t", new PrimitiveBound("0", "18446744073709551615"));

        // STDint type
        addStdIntBound(bounds);

        return bounds;
    }

    public DataSizeModel createLLP64() {
        DataSizeModel bounds = new DataSizeModel();

        // Character types
        bounds.put("char", createSignedIntBound(1));
        bounds.put("signed char", bounds.get("char"));
        bounds.put("unsigned char", createUnsignedIntBound(1));
        bounds.put("wchar_t", createUnsignedIntBound(4));
        bounds.put("char8_t", createUnsignedIntBound(1));
        bounds.put("char16_t", createUnsignedIntBound(2));
        bounds.put("char32_t", createUnsignedIntBound(4));

        // Integer types
        bounds.put("short", createSignedIntBound(2));
        bounds.put("short int", bounds.get("short"));
        bounds.put("signed short", bounds.get("short"));
        bounds.put("signed short int", bounds.get("short"));
        bounds.put("unsigned short", createUnsignedIntBound(2));
        bounds.put("unsigned short int", bounds.get("unsigned short"));

        bounds.put("int", createSignedIntBound(4));
        bounds.put("signed", bounds.get("int"));
        bounds.put("signed int", bounds.get("int"));
        bounds.put("unsigned", createUnsignedIntBound(4));
        bounds.put("unsigned int", bounds.get("unsigned"));//

        bounds.put("long", createSignedIntBound(4));
        bounds.put("long int", bounds.get("long"));
        bounds.put("signed long", bounds.get("long"));
        bounds.put("signed long int", bounds.get("long"));
        bounds.put("unsigned long", createUnsignedIntBound(4));
        bounds.put("unsigned long int", bounds.get("unsigned long"));

        bounds.put("long long", createSignedIntBound(8));
        bounds.put("long long int", bounds.get("long long"));
        bounds.put("signed long long", bounds.get("long long"));
        bounds.put("signed long long int", bounds.get("long long"));
        bounds.put("unsigned long long", new PrimitiveBound("0", "18446744073709551615"));
        bounds.put("unsigned long long int", bounds.get("unsigned long long"));

        // Floating-point types
        bounds.put("float", new PrimitiveBound(FLOAT_MIN_VALUE, FLOAT_MAX_VALUE));
        bounds.put("double", new PrimitiveBound(DOUBLE_MIN_VALUE, DOUBLE_MAX_VALUE));
        bounds.put("long double", new PrimitiveBound(LONG_DOUBLE_MIN_VALUE, LONG_DOUBLE_MAX_VALUE)); // 80bit x87 on x86 and x86-64 architecture

        // Boolean type
        bounds.put("bool", createSignedIntBound(1));

        // Size_t
        bounds.put("size_t", new PrimitiveBound("0", "18446744073709551615"));

        // STDint type
        addStdIntBound(bounds);

        return bounds;
    }

    public DataSizeModel createILP32() {
        DataSizeModel bounds = new DataSizeModel();

        // Character types
        bounds.put("char", createSignedIntBound(1));
        bounds.put("signed char", bounds.get("char"));
        bounds.put("unsigned char", createUnsignedIntBound(1));
        bounds.put("wchar_t", createUnsignedIntBound(2));
        bounds.put("char8_t", createUnsignedIntBound(1));
        bounds.put("char16_t", createUnsignedIntBound(2));
        bounds.put("char32_t", createUnsignedIntBound(4));

        // Integer types
        bounds.put("short", createSignedIntBound(2));
        bounds.put("short int", bounds.get("short"));
        bounds.put("signed short", bounds.get("short"));
        bounds.put("signed short int", bounds.get("short"));
        bounds.put("unsigned short", createUnsignedIntBound(2));
        bounds.put("unsigned short int", bounds.get("unsigned short"));

        bounds.put("int", createSignedIntBound(4));
        bounds.put("signed", bounds.get("int"));
        bounds.put("signed int", bounds.get("int"));
        bounds.put("unsigned", createUnsignedIntBound(4));
        bounds.put("unsigned int", bounds.get("unsigned"));//

        bounds.put("long", createSignedIntBound(4));
        bounds.put("long int", bounds.get("long"));
        bounds.put("signed long", bounds.get("long"));
        bounds.put("signed long int", bounds.get("long"));
        bounds.put("unsigned long", createUnsignedIntBound(4));
        bounds.put("unsigned long int", bounds.get("unsigned long"));

        bounds.put("long long", createSignedIntBound(8));
        bounds.put("long long int", bounds.get("long long"));
        bounds.put("signed long long", bounds.get("long long"));
        bounds.put("signed long long int", bounds.get("long long"));
        bounds.put("unsigned long long", new PrimitiveBound("0", "18446744073709551615"));
        bounds.put("unsigned long long int", bounds.get("unsigned long long"));

        // Floating-point types
        bounds.put("float", new PrimitiveBound(FLOAT_MIN_VALUE, FLOAT_MAX_VALUE));
        bounds.put("double", new PrimitiveBound(DOUBLE_MIN_VALUE, DOUBLE_MAX_VALUE));
        bounds.put("long double", new PrimitiveBound(LONG_DOUBLE_MIN_VALUE, LONG_DOUBLE_MAX_VALUE)); // 80bit x87 on x86 and x86-64 architecture

        // Boolean type
        bounds.put("bool", createSignedIntBound(1));

        // Size_t
        bounds.put("size_t", createUnsignedIntBound(4));

        // STDint type
        addStdIntBound(bounds);

        return bounds;
    }

    public DataSizeModel createLP32() {
        DataSizeModel bounds = new DataSizeModel();

        // Character types
        bounds.put("char", createSignedIntBound(1));
        bounds.put("signed char", bounds.get("char"));
        bounds.put("unsigned char", createUnsignedIntBound(1));
        bounds.put("wchar_t", createUnsignedIntBound(2));
        bounds.put("char8_t", createUnsignedIntBound(1));
        bounds.put("char16_t", createUnsignedIntBound(2));
        bounds.put("char32_t", createUnsignedIntBound(4));

        // Integer types
        bounds.put("short", createSignedIntBound(2));
        bounds.put("short int", bounds.get("short"));
        bounds.put("signed short", bounds.get("short"));
        bounds.put("signed short int", bounds.get("short"));
        bounds.put("unsigned short", createUnsignedIntBound(2));
        bounds.put("unsigned short int", bounds.get("unsigned short"));

        bounds.put("int", createSignedIntBound(2));
        bounds.put("signed", bounds.get("int"));
        bounds.put("signed int", bounds.get("int"));
        bounds.put("unsigned", createUnsignedIntBound(2));
        bounds.put("unsigned int", bounds.get("unsigned"));//

        bounds.put("long", createSignedIntBound(4));
        bounds.put("long int", bounds.get("long"));
        bounds.put("signed long", bounds.get("long"));
        bounds.put("signed long int", bounds.get("long"));
        bounds.put("unsigned long", createUnsignedIntBound(4));
        bounds.put("unsigned long int", bounds.get("unsigned long"));

        bounds.put("long long", createSignedIntBound(8));
        bounds.put("long long int", bounds.get("long long"));
        bounds.put("signed long long", bounds.get("long long"));
        bounds.put("signed long long int", bounds.get("long long"));
        bounds.put("unsigned long long", new PrimitiveBound("0", "18446744073709551615"));
        bounds.put("unsigned long long int", bounds.get("unsigned long long"));

        // Floating-point types
        bounds.put("float", new PrimitiveBound(FLOAT_MIN_VALUE, FLOAT_MAX_VALUE));
        bounds.put("double", new PrimitiveBound(DOUBLE_MIN_VALUE, DOUBLE_MAX_VALUE));
        bounds.put("long double", new PrimitiveBound(LONG_DOUBLE_MIN_VALUE, LONG_DOUBLE_MAX_VALUE)); // 80bit x87 on x86 and x86-64 architecture

        // Boolean type
        bounds.put("bool", createSignedIntBound(1));

        // Size_t
        bounds.put("size_t", createUnsignedIntBound(4));

        // STDint type
        addStdIntBound(bounds);

        return bounds;
    }


    /**
     * Ref: https://www.cplusplus.com/reference/cstdint/
     */
    public void addStdIntBound(DataSizeModel bounds) {
        bounds.put("intmax_t", createSignedIntBound(8));
        bounds.put("uintmax_t", new PrimitiveBound("0", "18446744073709551615"));
        bounds.put("int8_t", createSignedIntBound(1));
        bounds.put("uint8_t", createUnsignedIntBound(1));
        bounds.put("int16_t", createSignedIntBound(2));
        bounds.put("uint16_t", createUnsignedIntBound(2));
        bounds.put("int32_t", createSignedIntBound(4));
        bounds.put("uint32_t", createUnsignedIntBound(4));
        bounds.put("int64_t", createSignedIntBound(8));
        bounds.put("uint64_t", new PrimitiveBound("0", "18446744073709551615"));
        bounds.put("int_least8_t", createSignedIntBound(1));
        bounds.put("uint_least8_t", createUnsignedIntBound(1));
        bounds.put("int_least16_t", createSignedIntBound(2));
        bounds.put("uint_least16_t", createUnsignedIntBound(2));
        bounds.put("int_least32_t", createSignedIntBound(4));
        bounds.put("uint_least32_t", createUnsignedIntBound(4));
        bounds.put("int_least64_t", createSignedIntBound(8));
        bounds.put("uint_least64_t", new PrimitiveBound("0", "18446744073709551615"));
        bounds.put("int_fast8_t", createSignedIntBound(1));
        bounds.put("uint_fast8_t", createUnsignedIntBound(1));
        bounds.put("int_fast16_t", createSignedIntBound(2));
        bounds.put("uint_fast16_t", createUnsignedIntBound(2));
        bounds.put("int_fast32_t", createSignedIntBound(4));
        bounds.put("uint_fast32_t", createUnsignedIntBound(4));
        bounds.put("int_fast64_t", createSignedIntBound(8));
        bounds.put("uint_fast64_t", new PrimitiveBound("0", "18446744073709551615"));
    }

    private static PrimitiveBound createSignedIntBound(int bytes) {
        int bits = bytes * 8;
        long upper = 1;
        for (int i = 0; i < bits - 1; i++) {
            upper *= 2;
        }
        long lower = upper * -1;
        upper -= 1;
        return new PrimitiveBound(lower, upper);
    }

    private static PrimitiveBound createUnsignedIntBound(int bytes) {
        int bits = bytes * 8;
        long upper = 1;
        for (int i = 0; i < bits; i++) {
            upper *= 2;
        }
        long lower = 0;
        upper -= 1;
        return new PrimitiveBound(lower, upper);
    }

    public DataSizeModel getBounds() {
        return bounds;
    }

    public void setBounds(DataSizeModel bounds) {
        this.bounds = bounds;
    }

    public static void main(String[] args) {
        BoundOfDataTypes types = new BoundOfDataTypes();
        DataSizeModel model = types.createILP32();
        System.out.println();
    }
}