package org.ecorefs.eval.rq2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Groups the files of the call links into connected components, where two files
 * belong together when one calls the other. Each component becomes one group of
 * IPFS resources with references between them.
 */
final class CallComponents {

    private final Map<String, String> parent = new HashMap<>();

    /**
     * Prevents instances outside {@link #of(List)}.
     */
    private CallComponents() {
    }

    /**
     * Groups the files of the links.
     *
     * @param links the call links
     * @return the files of each component, sorted, and the components in the order of their first file
     */
    static List<List<String>> of(List<CallLink> links) {
        CallComponents components = new CallComponents();
        for (CallLink link : links) {
            components.join(link.caller, link.callee);
        }
        Map<String, TreeSet<String>> byRoot = new TreeMap<>();
        for (String file : components.parent.keySet()) {
            String root = components.rootOf(file);
            TreeSet<String> members = byRoot.get(root);
            if (members == null) {
                members = new TreeSet<>();
                byRoot.put(root, members);
            }
            members.add(file);
        }
        List<List<String>> result = new ArrayList<>();
        for (TreeSet<String> members : byRoot.values()) {
            result.add(new ArrayList<>(members));
        }
        result.sort(new FirstFileOrder());
        return result;
    }

    /**
     * Puts two files into the same component.
     *
     * @param first  one file
     * @param second the other file
     */
    private void join(String first, String second) {
        String firstRoot = rootOf(first);
        String secondRoot = rootOf(second);
        if (!firstRoot.equals(secondRoot)) {
            parent.put(firstRoot, secondRoot);
        }
    }

    /**
     * Finds the representative of the component of a file.
     *
     * @param file the file
     * @return the representative file
     */
    private String rootOf(String file) {
        parent.putIfAbsent(file, file);
        String current = file;
        while (!parent.get(current).equals(current)) {
            current = parent.get(current);
        }
        return current;
    }

    /**
     * Orders components by their first file.
     */
    private static final class FirstFileOrder implements Comparator<List<String>> {

        /**
         * Compares the first files of two components.
         *
         * @param first  one component
         * @param second the other component
         * @return the order of the first files
         */
        @Override
        public int compare(List<String> first, List<String> second) {
            return first.get(0).compareTo(second.get(0));
        }
    }
}
