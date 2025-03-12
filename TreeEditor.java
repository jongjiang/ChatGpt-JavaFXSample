import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
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
    // Updated Gson: disable HTML escaping so Unicode characters are not escaped.
    private Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public TreeEditor() {
        super("Multi-line Tree with UTF-8 JSON Export, Black Border, Dialog Rename & Node Hierarchy Moves");

        // Create the root node and initialize the tree model.
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        // Disable inline editing – we use a dialog for renaming.
        tree.setEditable(false);
        // Allow variable row heights.
        tree.setRowHeight(0);

        // Set custom cell renderer to support multi-line text with a black border.
        tree.setCellRenderer(new MultiLineTreeCellRenderer());

        // Place the tree in a scroll pane.
        JScrollPane scrollPane = new JScrollPane(tree);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        // Create a popup menu with various options.
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem addSibling = new JMenuItem("Add Sibling");
        JMenuItem addChild = new JMenuItem("Add Child");
        JMenuItem renameNode = new JMenuItem("Rename Node");
        JMenuItem deleteNode = new JMenuItem("Delete Node");
        JMenuItem moveUp = new JMenuItem("Move Up");
        JMenuItem moveDown = new JMenuItem("Move Down");
        JMenuItem promoteNode = new JMenuItem("Promote Node");
        JMenuItem demoteNode = new JMenuItem("Demote Node");

        popupMenu.add(addSibling);
        popupMenu.add(addChild);
        popupMenu.add(renameNode);
        popupMenu.add(deleteNode);
        popupMenu.addSeparator();
        popupMenu.add(moveUp);
        popupMenu.add(moveDown);
        popupMenu.addSeparator();
        popupMenu.add(promoteNode);
        popupMenu.add(demoteNode);

        tree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showPopup(e);
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showPopup(e);
            }
            private void showPopup(MouseEvent e) {
                int row = tree.getRowForLocation(e.getX(), e.getY());
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (row != -1) {
                    tree.setSelectionPath(path);
                    popupMenu.show(tree, e.getX(), e.getY());
                }
            }
        });

        // -------------------------
        // Node Operations
        // -------------------------
        // Add Child.
        addChild.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if (selectedPath == null) {
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to add a child.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode("New Child");
            treeModel.insertNodeInto(newNode, selectedNode, selectedNode.getChildCount());
            tree.expandPath(selectedPath);
        });

        // Add Sibling.
        addSibling.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if (selectedPath == null) {
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to add a sibling.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            if (parent == null) {
                JOptionPane.showMessageDialog(TreeEditor.this, "Root node cannot have siblings.");
                return;
            }
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode("New Sibling");
            int index = parent.getIndex(selectedNode) + 1;
            treeModel.insertNodeInto(newNode, parent, index);
            tree.expandPath(new TreePath(parent.getPath()));
        });

        // Rename Node via a dialog.
        renameNode.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if (selectedPath == null) {
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to rename.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            String currentText = selectedNode.getUserObject().toString().replaceAll("(?i)<br>", "\n");
            JTextArea textArea = new JTextArea(10, 30);
            textArea.setText(currentText);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            JScrollPane sp = new JScrollPane(textArea);
            int option = JOptionPane.showConfirmDialog(TreeEditor.this, sp, "Edit Node Text",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option == JOptionPane.OK_OPTION) {
                String newText = textArea.getText();
                selectedNode.setUserObject(newText);
                treeModel.nodeChanged(selectedNode);
                SwingUtilities.invokeLater(() -> {
                    tree.setRowHeight(0);
                    tree.revalidate();
                    tree.repaint();
                });
            }
        });

        // Delete Node.
        deleteNode.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if (selectedPath == null) {
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to delete.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (selectedNode.isRoot()) {
                JOptionPane.showMessageDialog(TreeEditor.this, "Cannot delete the root node.");
                return;
            }
            treeModel.removeNodeFromParent(selectedNode);
        });

        // Move Up (among siblings).
        moveUp.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if (selectedPath == null) {
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to move up.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            if (parent == null) {
                JOptionPane.showMessageDialog(TreeEditor.this, "Cannot move the root node.");
                return;
            }
            int index = parent.getIndex(selectedNode);
            if (index > 0) {
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
            if (selectedPath == null) {
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to move down.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            if (parent == null) {
                JOptionPane.showMessageDialog(TreeEditor.this, "Cannot move the root node.");
                return;
            }
            int index = parent.getIndex(selectedNode);
            if (index < parent.getChildCount() - 1) {
                treeModel.removeNodeFromParent(selectedNode);
                treeModel.insertNodeInto(selectedNode, parent, index + 1);
                TreePath newPath = new TreePath(selectedNode.getPath());
                tree.setSelectionPath(newPath);
                tree.scrollPathToVisible(newPath);
            } else {
                JOptionPane.showMessageDialog(TreeEditor.this, "Node is already at the bottom among its siblings.");
            }
        });

        // Promote Node (move up one level in the hierarchy).
        promoteNode.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if (selectedPath == null) {
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to promote.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            if (parent == null || parent.isRoot()) {
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

        // Demote Node (move down one level in the hierarchy).
        demoteNode.addActionListener(e -> {
            TreePath selectedPath = tree.getSelectionPath();
            if (selectedPath == null) {
                JOptionPane.showMessageDialog(TreeEditor.this, "Please select a node to demote.");
                return;
            }
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            if (parent == null) {
                JOptionPane.showMessageDialog(TreeEditor.this, "Cannot demote the root node.");
                return;
            }
            int index = parent.getIndex(selectedNode);
            if (index == 0) {
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

        // -------------------------
        // File Menu: JSON Export/Import, Expand/Collapse
        // -------------------------
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

        // Export JSON action using UTF-8.
        exportJson.addActionListener(e -> {
            NodeData data = convertToNodeData((DefaultMutableTreeNode) treeModel.getRoot());
            String jsonString = gson.toJson(data);
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showSaveDialog(TreeEditor.this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try (OutputStreamWriter writer = new OutputStreamWriter(
                        new FileOutputStream(file), "UTF-8")) {
                    writer.write(jsonString);
                    JOptionPane.showMessageDialog(TreeEditor.this, "Export successful!");
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(TreeEditor.this, "Error writing file: " + ex.getMessage());
                }
            }
        });

        // Import JSON action.
        importJson.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showOpenDialog(TreeEditor.this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
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
    // Custom Multi-line Renderer with Black Border
    // -------------------------
    private static class MultiLineTreeCellRenderer extends JTextArea implements TreeCellRenderer {
        public MultiLineTreeCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            // Compound border: black line with inner empty padding.
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2)
            ));
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            String text = value.toString().replaceAll("(?i)<br>", "\n");
            setText(text);
            setFont(tree.getFont());
            if (selected) {
                setBackground(UIManager.getColor("Tree.selectionBackground"));
                setForeground(UIManager.getColor("Tree.selectionForeground"));
            } else {
                setBackground(UIManager.getColor("Tree.textBackground"));
                setForeground(UIManager.getColor("Tree.textForeground"));
            }
            int treeWidth = tree.getWidth() > 0 ? tree.getWidth() : 100;
            setSize(treeWidth, Short.MAX_VALUE);
            return this;
        }
    }

    // -------------------------
    // Expand/Collapse Methods
    // -------------------------
    private void expandAll(JTree tree, TreePath parent) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            TreePath path = parent.pathByAddingChild(child);
            expandAll(tree, path);
        }
        tree.expandPath(parent);
    }

    private void collapseAll(JTree tree, TreePath parent) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            TreePath path = parent.pathByAddingChild(child);
            collapseAll(tree, path);
        }
        tree.collapsePath(parent);
    }

    // -------------------------
    // JSON Conversion Helpers
    // -------------------------
    private static class NodeData {
        String text;
        List<NodeData> children = new ArrayList<>();
    }

    private NodeData convertToNodeData(DefaultMutableTreeNode node) {
        NodeData data = new NodeData();
        data.text = node.getUserObject().toString();
        for (int i = 0; i < node.getChildCount(); i++) {
            data.children.add(convertToNodeData((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        return data;
    }

    private DefaultMutableTreeNode convertToTreeNode(NodeData data) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(data.text);
        for (NodeData childData : data.children) {
            node.add(convertToTreeNode(childData));
        }
        return node;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TreeEditor().setVisible(true));
    }
}
