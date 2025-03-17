import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SuppressWarnings("serial")
public class TreeEditor extends JFrame {
    private JTree tree;
    private DefaultTreeModel treeModel;
    // Gson configured to produce pretty JSON without escaping Unicode.
    private Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public TreeEditor() {
        super("Tree Editor with Color, Hierarchy Moves & Tab Indentation");

        // Create the root node using a NodeInfo (default text with black color).
        NodeInfo rootInfo = new NodeInfo("Root", "#000000");
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootInfo);
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setEditable(false); // We use dialogs for editing.
        tree.setRowHeight(0);    // Allow variable row heights.

        // Use custom cell renderer that displays multi-line text, a black border, and the node's font color.
        tree.setCellRenderer(new MultiLineTreeCellRenderer());

        // Place the tree in a scroll pane.
        JScrollPane scrollPane = new JScrollPane(tree);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        // Create a popup menu with various options.
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem addSibling = new JMenuItem("Add Sibling");
        JMenuItem addChild = new JMenuItem("Add Child");
        JMenuItem renameNode = new JMenuItem("Rename Node");
        JMenuItem changeColor = new JMenuItem("Change Font Color");
        JMenuItem deleteNode = new JMenuItem("Delete Node");
        JMenuItem moveUp = new JMenuItem("Move Up");
        JMenuItem moveDown = new JMenuItem("Move Down");
        JMenuItem promoteNode = new JMenuItem("Promote Node");
        JMenuItem demoteNode = new JMenuItem("Demote Node");
        popupMenu.add(addSibling);
        popupMenu.add(addChild);
        popupMenu.add(renameNode);
        popupMenu.add(changeColor);
        popupMenu.add(deleteNode);
        popupMenu.addSeparator();
        popupMenu.add(moveUp);
        popupMenu.add(moveDown);
        popupMenu.addSeparator();
        popupMenu.add(promoteNode);
        popupMenu.add(demoteNode);

        tree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if(e.isPopupTrigger()){
                    showPopup(e);
                }
            }
            public void mouseReleased(MouseEvent e) {
                if(e.isPopupTrigger()){
                    showPopup(e);
                }
            }
            private void showPopup(MouseEvent e) {
                int row = tree.getRowForLocation(e.getX(), e.getY());
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if(row != -1) {
                    tree.setSelectionPath(path);
                    popupMenu.show(tree, e.getX(), e.getY());
                }
            }
        });

        // ----- Node Operations -----
        // Add Child.
        addChild.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if(selectedPath == null){
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to add a child.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            NodeInfo newInfo = new NodeInfo("New Child", "#000000");
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newInfo);
            treeModel.insertNodeInto(newNode, selectedNode, selectedNode.getChildCount());
            tree.expandPath(selectedPath);
        });

        // Add Sibling.
        addSibling.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if(selectedPath == null){
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to add a sibling.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            if(parent == null){
                JOptionPane.showMessageDialog(TreeEditor.this, "Root node cannot have siblings.");
                return;
            }
            NodeInfo newInfo = new NodeInfo("New Sibling", "#000000");
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newInfo);
            int index = parent.getIndex(selectedNode) + 1;
            treeModel.insertNodeInto(newNode, parent, index);
            tree.expandPath(new TreePath(parent.getPath()));
        });

        // Rename Node via a dialog (with custom Tab and Shift+Tab behavior).
        renameNode.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if(selectedPath == null){
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to rename.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            NodeInfo info = (NodeInfo) selectedNode.getUserObject();
            // Convert any <br> to newline.
            String currentText = info.text.replaceAll("(?i)<br>", "\n");
            JTextArea textArea = new JTextArea(10, 30);
            textArea.setText(currentText);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            // Enable tab insertion (indent/dedent) by installing custom key bindings.
            setupTabKeyBindings(textArea);
            JScrollPane sp = new JScrollPane(textArea);
            int option = JOptionPane.showConfirmDialog(TreeEditor.this, sp, "Edit Node Text", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if(option == JOptionPane.OK_OPTION){
                String newText = textArea.getText();
                info.text = newText;
                treeModel.nodeChanged(selectedNode);
                SwingUtilities.invokeLater(() -> {
                    tree.setRowHeight(0);
                    tree.revalidate();
                    tree.repaint();
                });
            }
        });

        // Change Font Color.
        changeColor.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if(selectedPath == null){
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to change its color.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            NodeInfo info = (NodeInfo) selectedNode.getUserObject();
            Color currentColor;
            try {
                currentColor = Color.decode(info.color);
            } catch(Exception ex){
                currentColor = Color.BLACK;
            }
            Color chosen = JColorChooser.showDialog(TreeEditor.this, "Choose Font Color", currentColor);
            if(chosen != null){
                String hex = String.format("#%06X", (0xFFFFFF & chosen.getRGB()));
                info.color = hex;
                treeModel.nodeChanged(selectedNode);
                tree.repaint();
            }
        });

        // Delete Node.
        deleteNode.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if(selectedPath == null){
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to delete.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if(selectedNode.isRoot()){
                JOptionPane.showMessageDialog(TreeEditor.this, "Cannot delete the root node.");
                return;
            }
            treeModel.removeNodeFromParent(selectedNode);
        });

        // Move Up (among siblings).
        moveUp.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if(selectedPath == null){
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to move up.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            if(parent == null){
                JOptionPane.showMessageDialog(TreeEditor.this, "Cannot move the root node.");
                return;
            }
            int index = parent.getIndex(selectedNode);
            if(index > 0){
                treeModel.removeNodeFromParent(selectedNode);
                treeModel.insertNodeInto(selectedNode, parent, index - 1);
                TreePath newPath = new TreePath(selectedNode.getPath());
                tree.setSelectionPath(newPath);
                tree.scrollPathToVisible(newPath);
            } else {
                JOptionPane.showMessageDialog(TreeEditor.this, "Node is already at the top among its siblings.");
            }
        });

        // Move Down (among siblings).
        moveDown.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if(selectedPath == null){
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to move down.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            if(parent == null){
                JOptionPane.showMessageDialog(TreeEditor.this, "Cannot move the root node.");
                return;
            }
            int index = parent.getIndex(selectedNode);
            if(index < parent.getChildCount() - 1){
                treeModel.removeNodeFromParent(selectedNode);
                treeModel.insertNodeInto(selectedNode, parent, index + 1);
                TreePath newPath = new TreePath(selectedNode.getPath());
                tree.setSelectionPath(newPath);
                tree.scrollPathToVisible(newPath);
            } else {
                JOptionPane.showMessageDialog(TreeEditor.this, "Node is already at the bottom among its siblings.");
            }
        });

        // Promote Node (move to an upper-level branch).
        promoteNode.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if(selectedPath == null){
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to promote.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            if(parent == null || parent.isRoot()){
                JOptionPane.showMessageDialog(TreeEditor.this, "Cannot promote this node further.");
                return;
            }
            DefaultMutableTreeNode grandparent = (DefaultMutableTreeNode) parent.getParent();
            int parentIndex = grandparent.getIndex(parent);
            treeModel.removeNodeFromParent(selectedNode);
            treeModel.insertNodeInto(selectedNode, grandparent, parentIndex + 1);
            TreePath newPath = new TreePath(selectedNode.getPath());
            tree.setSelectionPath(newPath);
            tree.scrollPathToVisible(newPath);
        });

        // Demote Node (move to a lower-level branch: become child of its previous sibling).
        demoteNode.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if(selectedPath == null){
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to demote.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            if(parent == null){
                JOptionPane.showMessageDialog(TreeEditor.this, "Cannot demote the root node.");
                return;
            }
            int index = parent.getIndex(selectedNode);
            if(index == 0){
                JOptionPane.showMessageDialog(TreeEditor.this, "No previous sibling exists to demote under.");
                return;
            }
            DefaultMutableTreeNode previousSibling = (DefaultMutableTreeNode) parent.getChildAt(index - 1);
            treeModel.removeNodeFromParent(selectedNode);
            treeModel.insertNodeInto(selectedNode, previousSibling, previousSibling.getChildCount());
            TreePath newPath = new TreePath(selectedNode.getPath());
            tree.setSelectionPath(newPath);
            tree.scrollPathToVisible(newPath);
        });

        // ----- File Menu: JSON Export/Import, Expand/Collapse -----
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exportJson = new JMenuItem("Export JSON");
        JMenuItem importJson = new JMenuItem("Import JSON");
        JMenuItem expandAll = new JMenuItem("Expand All");
        JMenuItem collapseAll = new JMenuItem("Collapse All");
        fileMenu.add(exportJson);
        fileMenu.add(importJson);
        fileMenu.addSeparator();
        fileMenu.add(expandAll);
        fileMenu.add(collapseAll);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        exportJson.addActionListener(e -> {
            NodeData data = convertToNodeData((DefaultMutableTreeNode) treeModel.getRoot());
            String jsonString = gson.toJson(data);
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showSaveDialog(TreeEditor.this);
            if(option == JFileChooser.APPROVE_OPTION){
                File file = fileChooser.getSelectedFile();
                try (OutputStreamWriter writer = new OutputStreamWriter(
                        new FileOutputStream(file), StandardCharsets.UTF_8)) {
                    writer.write(jsonString);
                    JOptionPane.showMessageDialog(TreeEditor.this, "Export successful!");
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(TreeEditor.this, "Error writing file: " + ex.getMessage());
                }
            }
        });

        importJson.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showOpenDialog(TreeEditor.this);
            if(option == JFileChooser.APPROVE_OPTION){
                File file = fileChooser.getSelectedFile();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) != null){
                        sb.append(line);
                    }
                    NodeData data = gson.fromJson(sb.toString(), NodeData.class);
                    DefaultMutableTreeNode newRoot = convertToTreeNode(data);
                    treeModel.setRoot(newRoot);
                    treeModel.reload();
                    JOptionPane.showMessageDialog(TreeEditor.this, "Import successful!");
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(TreeEditor.this, "Error reading file: " + ex.getMessage());
                }
            }
        });

        expandAll.addActionListener(e -> expandAll(tree, new TreePath(treeModel.getRoot())));
        collapseAll.addActionListener(e -> collapseAll(tree, new TreePath(treeModel.getRoot())));

        setSize(500, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    // -------------------------
    // Helper: Set up Tab and Shift+Tab key bindings in a JTextArea
    // -------------------------
    private static void setupTabKeyBindings(JTextArea textArea) {
        textArea.setFocusTraversalKeysEnabled(false); // Allow Tab in text area.
        InputMap im = textArea.getInputMap();
        ActionMap am = textArea.getActionMap();
        
        im.put(KeyStroke.getKeyStroke("TAB"), "insert-tab");
        am.put("insert-tab", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                JTextArea ta = (JTextArea) e.getSource();
                int start = ta.getSelectionStart();
                int end = ta.getSelectionEnd();
                if(start == end) {
                    // No selection: Insert tab character.
                    ta.insert("\t", start);
                    ta.setCaretPosition(start + 1);
                } else {
                    String selected = ta.getSelectedText();
                    String[] lines = selected.split("\n", -1);
                    StringBuilder sb = new StringBuilder();
                    for(int i = 0; i < lines.length; i++){
                        sb.append("\t").append(lines[i]);
                        if(i < lines.length - 1) {
                            sb.append("\n");
                        }
                    }
                    ta.replaceSelection(sb.toString());
                    ta.select(start, start + sb.length());
                }
            }
        });
        
        im.put(KeyStroke.getKeyStroke("shift TAB"), "remove-tab");
        am.put("remove-tab", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                JTextArea ta = (JTextArea) e.getSource();
                int start = ta.getSelectionStart();
                int end = ta.getSelectionEnd();
                if(start == end) {
                    int caret = ta.getCaretPosition();
                    if(caret > 0) {
                        try {
                            String before = ta.getText(caret - 1, 1);
                            if("\t".equals(before)){
                                ta.getDocument().remove(caret - 1, 1);
                            }
                        } catch(Exception ex){}
                    }
                } else {
                    String selected = ta.getSelectedText();
                    String[] lines = selected.split("\n", -1);
                    StringBuilder sb = new StringBuilder();
                    for(int i = 0; i < lines.length; i++){
                        if(lines[i].startsWith("\t")){
                            sb.append(lines[i].substring(1));
                        } else {
                            sb.append(lines[i]);
                        }
                        if(i < lines.length - 1) {
                            sb.append("\n");
                        }
                    }
                    ta.replaceSelection(sb.toString());
                    ta.select(start, start + sb.length());
                }
            }
        });
    }

    // -------------------------
    // Custom Multi-line Renderer with Black Border
    // -------------------------
    private static class MultiLineTreeCellRenderer extends JTextArea implements TreeCellRenderer {
        public MultiLineTreeCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            // Compound border: black line border with inner empty padding.
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(2,2,2,2)
            ));
        }
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            // Expect node's user object to be a NodeInfo.
            NodeInfo info;
            if(value instanceof DefaultMutableTreeNode){
                Object userObj = ((DefaultMutableTreeNode)value).getUserObject();
                if(userObj instanceof NodeInfo){
                    info = (NodeInfo) userObj;
                } else {
                    info = new NodeInfo(userObj.toString(), "#000000");
                }
            } else {
                info = new NodeInfo(value.toString(), "#000000");
            }
            // Replace <br> with newline.
            String text = info.text.replaceAll("(?i)<br>", "\n");
            setText(text);
            setFont(tree.getFont());
            if(selected){
                setBackground(UIManager.getColor("Tree.selectionBackground"));
                setForeground(UIManager.getColor("Tree.selectionForeground"));
            } else {
                setBackground(UIManager.getColor("Tree.textBackground"));
                try {
                    setForeground(Color.decode(info.color));
                } catch(Exception ex){
                    setForeground(UIManager.getColor("Tree.textForeground"));
                }
            }
            int treeWidth = tree.getWidth() > 0 ? tree.getWidth() : 100;
            setSize(treeWidth, Short.MAX_VALUE);
            return this;
        }
    }

    // -------------------------
    // JSON Conversion Helpers
    // -------------------------
    private static class NodeData {
        String text;
        String color;
        List<NodeData> children = new ArrayList<>();
    }
    private NodeData convertToNodeData(DefaultMutableTreeNode node) {
        NodeData data = new NodeData();
        Object userObj = node.getUserObject();
        if(userObj instanceof NodeInfo){
            NodeInfo info = (NodeInfo) userObj;
            data.text = info.text;
            data.color = info.color;
        } else {
            data.text = userObj.toString();
            data.color = "#000000";
        }
        for(int i = 0; i < node.getChildCount(); i++){
            data.children.add(convertToNodeData((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        return data;
    }
    private DefaultMutableTreeNode convertToTreeNode(NodeData data) {
        NodeInfo info = new NodeInfo(data.text, data.color);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(info);
        for(NodeData childData : data.children){
            node.add(convertToTreeNode(childData));
        }
        return node;
    }

    // -------------------------
    // NodeInfo: holds node text and font color (HTML color code)
    // -------------------------
    private static class NodeInfo {
        String text;
        String color; // e.g., "#FF0000"
        public NodeInfo(String text, String color) {
            this.text = text;
            this.color = color;
        }
        @Override
        public String toString() {
            return text;
        }
    }

    // -------------------------
    // Expand/Collapse Methods
    // -------------------------
    private void expandAll(JTree tree, TreePath parent) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
        for(int i = 0; i < node.getChildCount(); i++){
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            TreePath path = parent.pathByAddingChild(child);
            expandAll(tree, path);
        }
        tree.expandPath(parent);
    }
    private void collapseAll(JTree tree, TreePath parent) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
        for(int i = 0; i < node.getChildCount(); i++){
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            TreePath path = parent.pathByAddingChild(child);
            collapseAll(tree, path);
        }
        tree.collapsePath(parent);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TreeEditor().setVisible(true));
    }
}
