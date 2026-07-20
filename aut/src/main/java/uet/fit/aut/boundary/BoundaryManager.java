package uet.fit.aut.boundary;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BoundaryManager {

    private final Map<String, BoundOfDataTypes> nameToBoundaryMap = new HashMap<>();

    /**
     * Singleton pattern
     */
    private static BoundaryManager instance = null;

    public static BoundaryManager getInstance() {
        if (instance == null) {
            instance = new BoundaryManager();
        }
        return instance;
    }

    private String usingBoundaryName;

    private BoundaryManager() {
        loadExistedBoundaries();
    }

    private void loadExistedBoundaries() {
        usingBoundaryName = BoundOfDataTypes.MODEL_LP32; // make default LP32

        BoundOfDataTypes LP32 = new BoundOfDataTypes();
        LP32.setBounds(LP32.createLP32());
        LP32.getBounds().setName("LP32");
        nameToBoundaryMap.put(LP32.getBounds().getName(), LP32);

        BoundOfDataTypes LP64 = new BoundOfDataTypes();
        LP64.setBounds(LP64.createLP64());
        LP64.getBounds().setName("LP64");
        nameToBoundaryMap.put(LP64.getBounds().getName(), LP64);

        // handle when the using Boundary Name is not existed
        if (!nameToBoundaryMap.containsKey(usingBoundaryName)) {
            usingBoundaryName = BoundOfDataTypes.MODEL_LP32;
        }
    }

    public void setUsingBoundaryName(String usingBoundaryName) {
        this.usingBoundaryName = usingBoundaryName;
    }

    public String getUsingBoundaryName() {
        return usingBoundaryName;
    }

    public BoundOfDataTypes getUsingBoundOfDataTypes() {
        return nameToBoundaryMap.get(usingBoundaryName);
    }

    public Collection<BoundOfDataTypes> getExistedBoundaries() {
        return nameToBoundaryMap.values();
    }

    public Map<String, BoundOfDataTypes> getNameToBoundaryMap() {
        return nameToBoundaryMap;
    }

    public void clear() {
        nameToBoundaryMap.clear();
    }
}
