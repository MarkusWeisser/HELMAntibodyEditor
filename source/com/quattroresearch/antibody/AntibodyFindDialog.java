/*******************************************************************************
 * Copyright C 2015, quattro research GmbH, Roche pREDi (Roche Innovation Center Penzberg)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package com.quattroresearch.antibody;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

import org.roche.antibody.model.antibody.Antibody;
import org.roche.antibody.model.antibody.Peptide;
import org.roche.antibody.services.ConfigFileService;
import org.roche.antibody.services.PreferencesDefault;
import org.roche.antibody.services.PreferencesService;
import org.roche.antibody.services.UIService;
import org.roche.antibody.services.antibody.AntibodyService;
import org.roche.antibody.ui.components.AntibodyEditorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quattroresearch.antibody.DomainDetection.E_ErrorType;
import com.quattroresearch.antibody.exception.AutoconnectorConnectingException;
import com.quattroresearch.antibody.exception.OverlappingDomainsException;

/**
 * Performs domain-search and displays results <p> Reads .csv library with every start and starts the domain-search.
 * Found hits are displayed in separate rows for every chain and separate columns for every domain.
 * 
 * @author <b>Anne Mund</b>, quattro research GmbH
 * @author <b>Marco Lanig:</b> lanig AT quattro-research DOT com, quattro research GmbH
 * 
 */
public class AntibodyFindDialog extends JDialog {

  /** The Logger for this class */
  private static final Logger LOG = LoggerFactory
      .getLogger(AntibodyFindDialog.class);

  private static final long serialVersionUID = 8614034134161069413L;

  private AntibodyEditorDialog antibodyEditorDialog;

  private int tooltipDefaultDelayInitial;

  private int tooltipDefaultDelayReshow;

  private int tooltipDefaultDismissDelay;

  private JButton btnOK;

  private JButton btnGoBack;

  private JButton btnReAnnotate;

  private AntibodyEditorPane viewDialog;

  private DomainDetection domainDetection;

  AntibodyFindDialog(
      AntibodyEditorDialog antibodyEditorDialog, boolean modal) {

    super(antibodyEditorDialog.getParentFrame(), modal);
    saveTooltipDelays();
    modifyTooltipDelays();

    this.antibodyEditorDialog = antibodyEditorDialog;

    getContentPane().repaint();
  }

  public boolean processData(List<String> chainNames, List<String> chains) throws FileNotFoundException {
    domainDetection =
        new DomainDetection(chainNames, chains, PreferencesService.getInstance()/* , null */);

    try {
      if (domainDetection.makeBlastDatabase().equals(
          E_ErrorType.LOADLIB_FAILED)) {
        JOptionPane.showMessageDialog(antibodyEditorDialog.getParentFrame(),
            "Could not load Library File!", "Error",
            JOptionPane.ERROR_MESSAGE);
        LOG.error("Unable to load library, no search conducted!");
        return false;
      }
    } catch (FileNotFoundException e) {
      JOptionPane.showMessageDialog(antibodyEditorDialog.getParentFrame(), "Please check the filepath in the Antibody Editor Settings", "Domain definition file not found", JOptionPane.ERROR_MESSAGE);
      LOG.error("File not found. Blast database not created.");
      return false;
    } catch (Exception e) {
      JOptionPane.showMessageDialog(antibodyEditorDialog.getParentFrame(), "Unable to make blast database : "
          + e.getClass().getName()
          + "(" + e.getMessage() + ")", "Blast failed.", JOptionPane.ERROR_MESSAGE);
      LOG.error("Unable to make blast database: " + e.getClass().getName()
          + "(" + e.getMessage() + ")");
      return false;
    }

    try {
      domainDetection.loadData();
    } catch (Exception e) {
      JOptionPane.showMessageDialog(antibodyEditorDialog.getParentFrame(), "Exception occurred while loading domain data: "
          + e.getClass().getName()
          + "(" + e.getMessage() + ")", "Loading domains failed.", JOptionPane.ERROR_MESSAGE);
      LOG.error("Exception occurred while loading domain data: "
          + e.getClass().getName()
          + "(" + e.getMessage() + ")");
      return false;
    }
    refreshPanels();
    domainDetection.checkCompability();
    return true;
  }

  private void saveTooltipDelays() {
    tooltipDefaultDelayInitial = ToolTipManager.sharedInstance()
        .getInitialDelay();
    tooltipDefaultDelayReshow = ToolTipManager.sharedInstance()
        .getReshowDelay();
    tooltipDefaultDismissDelay = ToolTipManager.sharedInstance()
        .getDismissDelay();
  }

  private void modifyTooltipDelays() {
    ToolTipManager.sharedInstance().setInitialDelay(500);
    ToolTipManager.sharedInstance().setReshowDelay(0);
    ToolTipManager.sharedInstance().setDismissDelay(20000);
  }

  private void restoreTooltipDelays() {
    ToolTipManager.sharedInstance().setInitialDelay(
        this.tooltipDefaultDelayInitial);
    ToolTipManager.sharedInstance().setReshowDelay(
        this.tooltipDefaultDelayReshow);
    ToolTipManager.sharedInstance().setDismissDelay(
        this.tooltipDefaultDismissDelay);
  }

  private void initComponents() {
    btnOK = new JButton("Accept");
    btnGoBack = new JButton("Return to Input");
    btnReAnnotate = new JButton("Re-Annotate changed domains");

    btnOK.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        try {
          okButtonActionPerformed(evt);
        } catch (Exception e) {
          e.printStackTrace();
        }

      }
    });

    btnGoBack.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        backButtonActionPerformed(evt);
      }
    });

    btnReAnnotate.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        reannotateButtonActionPerformed(evt);
      }
    });

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("Antibody Domains Found");

    JLabel lblDomainLibSource =
        new JLabel("Domain Library: " + ConfigFileService.getInstance().getDomainLibFilename());
    JLabel lblMutationLibSource =
        new JLabel("Mutation Library: " + ConfigFileService.getInstance().getMutationLibraryFilename());
    JLabel lblAutoconnectorSource =
        new JLabel("Autoconnector: " + ConfigFileService.getInstance().getAutoConnectorFilename());

    JScrollPane scroller = domainDetection.createScrollPane(this);

    org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(
        getContentPane());
    getContentPane().setLayout(layout);
    layout.setAutocreateContainerGaps(true);
    layout.setAutocreateGaps(true);

    layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
        .add(org.jdesktop.layout.GroupLayout.TRAILING,
            layout.createSequentialGroup()
                .addContainerGap()
                .add(btnGoBack)
                .addPreferredGap(
                    org.jdesktop.layout.LayoutStyle.RELATED,
                    300, Short.MAX_VALUE).add(btnReAnnotate)
                .add(btnOK)
                .addContainerGap().addContainerGap())
        .add(scroller)
        .add(lblDomainLibSource).add(lblMutationLibSource).add(lblAutoconnectorSource));

    layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
        .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
            .add(scroller)
            .addPreferredGap(
                org.jdesktop.layout.LayoutStyle.RELATED)
            .add(lblDomainLibSource).add(lblMutationLibSource).add(lblAutoconnectorSource)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                .add(btnReAnnotate)
                .add(btnOK)
                .add(btnGoBack))
            .addContainerGap()
        )
        );
    // addAntibodyViewEditor();

    Dimension scrollerMaxDimension = new Dimension(Toolkit.getDefaultToolkit().getScreenSize());
    scrollerMaxDimension.setSize(scrollerMaxDimension.getWidth() - 100, scrollerMaxDimension.getHeight());
    Dimension scrollerMinDimension = new Dimension(scroller.getMinimumSize());
    scrollerMinDimension.setSize(300, 300);

    scroller.setMaximumSize(scrollerMaxDimension);
    scroller.setMinimumSize(scrollerMinDimension);

    pack();
  }

  private void okButtonActionPerformed(java.awt.event.ActionEvent evt) throws Exception {
    List<Peptide> peptideList = null;

    try {
      this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      peptideList = domainDetection.calculatePeptides(false);
    } catch (OverlappingDomainsException od) {
      JOptionPane.showMessageDialog(antibodyEditorDialog.getParentFrame(), "Some domains are in condflicted state. Please check them for overlaps.",
          "Building peptides failed.", JOptionPane.ERROR_MESSAGE);
      LOG.error("OverlappingDomainsException. Antibody not built.");

      return;
    } catch (Exception e) {
      JOptionPane.showMessageDialog(antibodyEditorDialog.getParentFrame(), "An error occurred while creating peptides.",
          "Building peptides failed.", JOptionPane.ERROR_MESSAGE);
      LOG.error(e.getMessage());

      return;
    } finally {
      this.setCursor(Cursor.getDefaultCursor());
    }

    if (peptideList == null) {
      JOptionPane.showMessageDialog(antibodyEditorDialog.getParentFrame(),
          "The selected domains are not compatible", "Error",
          JOptionPane.ERROR_MESSAGE);
    } else {

      // mutation detection
      try {
        try {
          this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          FindMutations.find(peptideList, ConfigFileService.getInstance().fetchMutationLibrary());
        } catch (SQLException e) {
          JOptionPane.showMessageDialog(antibodyEditorDialog.getParentFrame(), "A SQL Exception occurred."
              + System.getProperty("line.separator") + "(" + e.getMessage() + ")", "Mutation detection failed.",
              JOptionPane.ERROR_MESSAGE);
          StringWriter errors = new StringWriter();
          e.printStackTrace(new PrintWriter(errors));
          LOG.error("SQLError in mutation detection: " + errors.toString());
          return;
        } catch (Exception e) {
          JOptionPane.showMessageDialog(antibodyEditorDialog.getParentFrame(), "Mutation detection failed."
              + System.getProperty("line.separator") + "(" + e.getMessage() + ")", "Mutation detection failed.", JOptionPane.ERROR_MESSAGE);
          StringWriter errors = new StringWriter();
          e.printStackTrace(new PrintWriter(errors));
          LOG.error("Error in mutation detection: " + errors.toString());
          return;
        } finally {
          this.setCursor(Cursor.getDefaultCursor());
        }

        if (viewDialog == null) {
          viewDialog = UIService.getInstance().addAntibodyViewEditor(this);
        }
        this.setVisible(false);
        Antibody ab = AntibodyService.getInstance().create(peptideList);

        // set the antibody and enable go-back
        viewDialog.setModel(ab);
        viewDialog.setIsBackToDomainEditorEnabled(true);

        boolean isAutoconnectorDebugEnabled =
            (boolean) PreferencesService.getInstance().getUserPrefs()
                .getBoolean(org.roche.antibody.services.PreferencesService.DD_AUTOCONNECTOR_DEBUG_MODE, PreferencesDefault.DD_AUTOCONNECTOR_DEBUG_MODE);
        if (isAutoconnectorDebugEnabled) {
          String log = ab.getAutoconnectionLog();
          if (!log.isEmpty())
            JOptionPane.showMessageDialog(antibodyEditorDialog.getParentFrame(), log, "Autoconnection Log", JOptionPane.INFORMATION_MESSAGE);
        }

      } catch (AutoconnectorConnectingException e) {
        JOptionPane.showMessageDialog(antibodyEditorDialog.getParentFrame(), "Autoconnector Exception occurred: "
            + e.getMessage(), "Building antibody failed", JOptionPane.ERROR_MESSAGE);
      } catch (Exception e) {
        JOptionPane.showMessageDialog(antibodyEditorDialog.getParentFrame(), "Unknown error occurred while creating the antibody", "Building antibody failed", JOptionPane.ERROR_MESSAGE);
        LOG.error("Error occurred: " + e.getClass().getName() + "(" + e.getMessage() + ")");
      }

    }

    restoreTooltipDelays();
  }

  private void refreshPanels() {
    // domainDetection.saveSelectedItemValues();

    getContentPane().removeAll();

    domainDetection.refreshGaps();
    domainDetection.restoreSelectedItemValues();

    domainDetection.checkCompability();
    initComponents();

    modifyTooltipDelays();
    getContentPane().repaint();
  }

  private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {
    antibodyEditorDialog.setVisible(true);
    restoreTooltipDelays();
    this.dispose();
  }

  /**
   * Action that performs a reannotation of the domains that changed.
   * 
   * @param evt Action Event
   */
  private void reannotateButtonActionPerformed(java.awt.event.ActionEvent evt) {
    try {
      this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      domainDetection.reannotateChangedDomains();
    } catch (FileNotFoundException e) {
      JOptionPane.showMessageDialog(antibodyEditorDialog.getParentFrame(), "Please check the filepath in the Antibody Editor Settings", "Domain definition file not found", JOptionPane.ERROR_MESSAGE);
      LOG.error(e.getMessage());
      return;
    } catch (Exception e) {
      JOptionPane.showMessageDialog(antibodyEditorDialog.getParentFrame(), "Reannotation failed: " + e.getMessage(), "Reannotation failed.", JOptionPane.ERROR_MESSAGE);
      LOG.error(e.getMessage());
      return;

    } finally {
      this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    repaint();
  }

  @Override
  public void repaint() {
    super.repaint();

    // ML 2014-03-19: Repaint rebuilds the whole panel
    // TODO atm only called when a domain is removed, but no good style to
    // rebuild on repaint
    refreshPanels();
  }

}
