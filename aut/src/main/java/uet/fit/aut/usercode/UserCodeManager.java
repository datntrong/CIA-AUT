package uet.fit.aut.usercode;

import uet.fit.aut.usercode.objects.AbstractUserCode;
import uet.fit.aut.usercode.objects.ParameterUserCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserCodeManager {
    private final Map<Integer, ParameterUserCode> idToParamUserCodeMap = new HashMap<>();

    /**
     * Singleton pattern
     */
    private static UserCodeManager instance = null;

    public static UserCodeManager getInstance() {
        if (instance == null) {
            instance = new UserCodeManager();
        }
        return instance;
    }

    public void clear() {
        idToParamUserCodeMap.clear();
    }

    public List<AbstractUserCode> getAllExistedUserCode() {
        return new ArrayList<>(idToParamUserCodeMap.values());
    }

    public void putParamUserCode(ParameterUserCode userCode) {
        if (userCode != null && !idToParamUserCodeMap.containsKey(userCode.getId())) {
            idToParamUserCodeMap.put(userCode.getId(), userCode);
        }
    }

    public void putUserCode(AbstractUserCode userCode) {
        if (userCode instanceof ParameterUserCode)
            putParamUserCode((ParameterUserCode) userCode);
    }

    public ParameterUserCode getParamUserCodeById(int id) {
        return idToParamUserCodeMap.get(id);
    }

    public List<ParameterUserCode> getAllParameterUserCodes() {
        return new ArrayList<>(idToParamUserCodeMap.values());
    }

    public void removeParamUserCode(ParameterUserCode userCode) {
        idToParamUserCodeMap.remove(userCode.getId());
    }

    // user code type
    public final static String USER_CODE_TYPE_ALL = "ALL";
    public final static String USER_CODE_TYPE_PARAM = "PARAMETER USER CODE";
    public final static String USER_CODE_TYPE_TEST_CASE = "TEST CASE USER CODE";
    // user code folder path
    public final static String PARAM_FOLDER_NAME = "parameter";
    public final static String TEST_CASE_FOLDER_NAME = "testcase";
}
