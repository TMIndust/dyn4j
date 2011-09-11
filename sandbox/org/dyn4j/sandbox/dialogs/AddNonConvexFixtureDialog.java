package org.dyn4j.sandbox.dialogs;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;

import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.sandbox.panels.FixturePanel;
import org.dyn4j.sandbox.panels.NonConvexPolygonPanel;
import org.dyn4j.sandbox.panels.TransformPanel;

/**
 * Dialog to add a new fixture to an existing body.
 * @author William Bittle
 * @version 1.0.0
 * @since 1.0.0
 */
public class AddNonConvexFixtureDialog extends JDialog implements ActionListener {
	/** The version id */
	private static final long serialVersionUID = -1809110047704548125L;
	
	/** The dialog canceled flag */
	private boolean canceled = true;

	/** The fixture config panel */
	private FixturePanel pnlFixture;
	
	/** The transform config panel */
	private TransformPanel pnlTransform;
	
	/** The non-convex polygon panel */
	private NonConvexPolygonPanel pnlPolygon;

	/** The fixture used during configuration */
	private BodyFixture fixture;
	
	/**
	 * Full constructor.
	 * @param owner the dialog owner
	 * @param icon the icon image
	 * @param title the dialog title
	 */
	private AddNonConvexFixtureDialog(Window owner, Image icon, String title) {
		super(owner, title, ModalityType.APPLICATION_MODAL);
		
		if (icon != null) {
			this.setIconImage(icon);
		}
		
		this.pnlPolygon = new NonConvexPolygonPanel();
		
		Container container = this.getContentPane();
		
		GroupLayout layout = new GroupLayout(container);
		container.setLayout(layout);
		
		// create a text pane for the local transform tab
		JTextPane lblText = new JTextPane();
		lblText = new JTextPane();
		lblText.setContentType("text/html");
		lblText.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		lblText.setText(
				"<html>The local transform is used to move and rotate a fixture within " +
				"body coordinates, i.e. relative to the body's center of mass.  Unlike " +
				"the transform on the body, this transform is applied directly to the fixture's " +
				"shape data and therefore not 'saved' directly.</html>");
		lblText.setEditable(false);
		lblText.setPreferredSize(new Dimension(350, 120));
		
		// have to create it with an arbitrary shape
		this.fixture = new BodyFixture(Geometry.createCircle(1.0));
		this.fixture.setUserData("Fixture" + AddConvexFixtureDialog.N);
		this.pnlFixture = new FixturePanel(this, this.fixture);
		this.pnlTransform = new TransformPanel(lblText);
		
		JTabbedPane tabs = new JTabbedPane();
		
		tabs.addTab("Shape", this.pnlPolygon);
		tabs.addTab("Fixture", this.pnlFixture);
		tabs.addTab("Local Transform", this.pnlTransform);
		
		JButton btnCancel = new JButton("Cancel");
		JButton btnAdd = new JButton("Add");
		btnCancel.setActionCommand("cancel");
		btnAdd.setActionCommand("add");
		btnCancel.addActionListener(this);
		btnAdd.addActionListener(this);
		
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		layout.setHorizontalGroup(
				layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup()
						.addComponent(tabs)
						.addGroup(layout.createSequentialGroup()
								.addComponent(btnCancel)
								.addComponent(btnAdd))));
		layout.setVerticalGroup(
				layout.createSequentialGroup()
				.addComponent(tabs)
				.addGroup(layout.createParallelGroup()
						.addComponent(btnCancel)
						.addComponent(btnAdd)));
		
		this.pack();
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent event) {
		// check the action command
		if ("cancel".equals(event.getActionCommand())) {
			// if its canceled then set the canceled flag and
			// close the dialog
			this.setVisible(false);
			this.canceled = true;
		} else {
			// check the shape panel's input
			if (this.pnlPolygon.isValidInput()) {
				// check the fixture input
				if (this.pnlFixture.isValidInput()) {
					// check the transform input
					if (this.pnlTransform.isValidInput()) {
						// if its valid then close the dialog
						this.canceled = false;
						this.setVisible(false);
					} else {
						this.pnlTransform.showInvalidInputMessage(this);
					}
				} else {
					this.pnlFixture.showInvalidInputMessage(this);
				}
			} else {
				// if its not valid then show an error message
				this.pnlPolygon.showInvalidInputMessage(this);
			}
		}
	}
	
	/**
	 * Shows a decomposition dialog.
	 * <p>
	 * Returns null if the user canceled or closed the dialog.
	 * @param owner the dialog owner
	 * @param icon the icon image
	 * @param title the dialog title
	 * @return List&lt;BodyFixture&gt;
	 */
	public static final List<BodyFixture> show(Window owner, Image icon, String title) {
		AddNonConvexFixtureDialog dialog = new AddNonConvexFixtureDialog(owner, icon, title);
		dialog.setVisible(true);
		// control returns to this method when the dialog is closed
		
		// check the canceled flag
		if (!dialog.canceled) {
			// get the list of convex shapes
			List<Convex> shapes = dialog.pnlPolygon.getShapes();
			
			// get the general fixture (properties will be copied into all fixtures created)
			BodyFixture fixture = dialog.fixture;
			
			// apply any local transform
			Vector2 tx = dialog.pnlTransform.getTranslation();
			double a = dialog.pnlTransform.getRotation();
			
			List<BodyFixture> fixtures = new ArrayList<BodyFixture>(shapes.size());
			int i = 1;
			// create a fixture for each shape
			for (Convex convex : shapes) {
				BodyFixture bf = new BodyFixture(convex);
				
				// apply the local transform
				if (!tx.isZero()) {
					convex.translate(tx);
				}
				if (a != 0.0) {
					convex.rotate(a);
				}
				
				bf.setDensity(fixture.getDensity());
				bf.setFilter(fixture.getFilter());
				bf.setFriction(fixture.getFriction());
				bf.setRestitution(fixture.getRestitution());
				bf.setSensor(fixture.isSensor());
				bf.setUserData(fixture.getUserData() + "_" + i);
				
				fixtures.add(bf);
				i++;
			}
			
			// increment the fixture number
			synchronized (AddConvexFixtureDialog.class) {
				AddConvexFixtureDialog.N++;
			}
			
			return fixtures;
		}
		
		// if it was canceled then return null
		return null;
	}
}