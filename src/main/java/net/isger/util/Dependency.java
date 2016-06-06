package net.isger.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Dependency {

    public static final String ROOT = "root";

    private volatile transient List<Object> stayings;

    private List<Object> stays;

    private List<Object> nodes;

    private Map<Object, List<Object>> dependencies;

    private Map<Object, List<Object>> bedependencies;

    public Dependency() {
        stayings = new LinkedList<Object>();
        nodes = new LinkedList<Object>();
        stays = new LinkedList<Object>();
        dependencies = new HashMap<Object, List<Object>>();
        bedependencies = new HashMap<Object, List<Object>>();
    }

    public List<Object> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public void addNode(Object node, Object... dependencies) {
        addNode(node, Arrays.asList(dependencies));
    }

    public void addNode(Object node, List<Object> dependencies) {
        if (dependencies == null) {
            dependencies = new ArrayList<Object>();
        } else if (dependencies.contains(node)) {
            throw new IllegalArgumentException(
                    "(X) Dependencies cannot contain itself [" + node + "]");
        }
        dependencies = Helpers.getMerge(this.dependencies.get(node),
                dependencies);
        this.addDependencies(node, dependencies);
        this.setDependencies(node, dependencies);
    }

    /**
     * 添加依赖
     * 
     * @param node
     * @param dependencies
     */
    private void addDependencies(Object node, List<Object> dependencies) {
        this.dependencies.put(node, dependencies);
        List<Object> bedependencies;
        for (Object dependency : dependencies) {
            bedependencies = this.bedependencies.get(dependency);
            if (bedependencies == null) {
                this.bedependencies.put(dependency,
                        bedependencies = new ArrayList<Object>());
            } else if (bedependencies.contains(node)) {
                continue;
            }
            bedependencies.add(node);
        }
    }

    /**
     * 设置依赖
     * 
     * @param node
     * @param dependencies
     */
    private void setDependencies(Object node, List<Object> dependencies) {
        if (dependencies != null) {
            dependencies = Helpers.getSurplus(this.nodes, dependencies);
            if (dependencies.size() == 0) {
                this.setNode(node);
            } else {
                for (Object dependency : dependencies) {
                    this.addStay(dependency);
                }
                this.addStay(node);
            }
        }
    }

    /**
     * 设置节点
     * 
     * @param node
     */
    private void setNode(Object node) {
        if (!this.nodes.contains(node)) {
            this.nodes.add(node);
        }
        List<Object> bedependencies = this.bedependencies.get(node);
        if (bedependencies != null) {
            for (Object bedependency : bedependencies) {
                setDependencies(bedependency,
                        this.dependencies.get(bedependency));
            }
        }
        this.stays.remove(node);
    }

    private void addStay(Object node) {
        if (this.stayings.contains(node)) {
            throw new IllegalStateException("(X) Found the self-devourer: "
                    + this.stayings + " -> " + node);
        }
        this.stayings.add(node);
        this.nodes.remove(node);
        if (!this.stays.contains(node)) {
            this.stays.add(node);
        }
        List<Object> bedependencies = this.bedependencies.get(node);
        if (bedependencies != null) {
            for (Object bedependency : bedependencies) {
                this.addStay(bedependency);
            }
        }
        this.stayings.remove(node);
    }

}
