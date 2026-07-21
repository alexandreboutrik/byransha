package byransha.ui.swing.desktop.src.dashboard.swing;

import javax.swing.*;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import byransha.ui.swing.desktop.src.dashboard.core.model.LayoutNode;
import byransha.ui.swing.desktop.src.dashboard.core.model.PanelNode;
import byransha.ui.swing.desktop.src.dashboard.core.model.SplitNode;

public class DashboardViewBuilder {

	private final PanelDragManager dragManager;

	public DashboardViewBuilder() {
		this.dragManager = new PanelDragManager();
	}

	public JComponent build(LayoutNode node, DashboardActions actions, DragOverlay overlay) {
		if (node instanceof PanelNode panel) {
			return buildPanel(panel, actions, overlay);
		}
		if (node instanceof SplitNode split) {
			return buildSplit(split, actions, overlay);
		}
		throw new RuntimeException("Unknown node");
	}

	private JComponent buildPanel(PanelNode panel, DashboardActions actions, DragOverlay overlay) {

		JButton add = new JButton("+");
		JButton close = new JButton("X");

		Dimension size = new Dimension(20, 20);
		add.setPreferredSize(size);
		close.setPreferredSize(size);
		Insets margin = new Insets(0, 0, 0, 0);
		add.setMargin(margin);
		close.setMargin(margin);
		add.setFocusPainted(false);
		close.setFocusPainted(false);
		add.setContentAreaFilled(false);
		close.setContentAreaFilled(false);

		add.addActionListener(e -> {
			actions.splitPanel(panel, true);
		});

		close.addActionListener(e -> {
			actions.closePanel(panel);
		});

		JPanel panelPane = new JPanel(new BorderLayout());
		panelPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		panelPane.setBackground(Color.LIGHT_GRAY);
		panelPane.putClientProperty("panelNode", panel);

		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(Color.DARK_GRAY);
		header.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		header.setPreferredSize(new Dimension(0, 30));

		SwingUtilities.invokeLater(() -> {
			dragManager.makeDraggable(header, panel, actions, overlay);
		});

		JLabel title = new JLabel(panel.getPanelName(), JLabel.CENTER);
		title.setForeground(Color.WHITE);

		header.add(title, BorderLayout.CENTER);
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

		buttonsPanel.add(add, BorderLayout.LINE_END);
		buttonsPanel.add(close, BorderLayout.LINE_END);

		header.add(buttonsPanel, BorderLayout.LINE_END);

		JPanel content = new JPanel();
		content.setBackground(Color.WHITE);

		// content.add(...);

		panelPane.add(header, BorderLayout.NORTH);
		panelPane.add(content, BorderLayout.CENTER);

		return panelPane;
	}

	private JComponent buildSplit(SplitNode split, DashboardActions actions, DragOverlay overlay) {
		int orientation = (split.getOrientation()) ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT;

		JPopupMenu menu = new JPopupMenu();

		JMenuItem rotate = new JMenuItem("Rotate");

		menu.add(rotate);

		rotate.addActionListener(e -> {
			actions.rotateSplit(split);
			System.out.println("rotate");
		});

		JSplitPane splitPane = new JSplitPane(orientation);

		for (LayoutNode child : split.getChildren()) {
			splitPane.add(build(child, actions, overlay));
		}

		splitPane.setResizeWeight(0.5);
		// A FAIRE poucentage au lieu de valeur fixes compliqué
		if (split.getDividerLocation() > 0) {
			// SwingUtilities.invokeLater(() -> {
			// int totalWidth = splitPane.getWidth(); // ou getHeight
			// double dividerPercentage = (double) split.getDividerLocation() / totalWidth;
			// System.out.println((double) split.getDividerLocation());
			// System.out.println(splitPane.getWidth());
			// splitPane.setDividerLocation(dividerPercentage);
			// });
			splitPane.setDividerLocation(split.getDividerLocation());

		}
		// splitPane.setDividerLocation(0.5);

		splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent pce) {
				actions.setDividerLocation(split, splitPane.getDividerLocation());
				// System.out.println(splitPane.getWidth());
			}
		});

		splitPane.setComponentPopupMenu(menu);
		return splitPane;
	}
}