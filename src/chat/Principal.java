/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;

import java.net.ServerSocket;
import java.net.Socket;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jorge
 */
public class Principal extends javax.swing.JFrame {

    private ServerSocket serverSocket;
    private Socket socket;
    
    private DataOutputStream flujoSalida;
    private DataInputStream flujoEntrada;
    
    private boolean tipoConexion; //true = servidor | false = cliente
    private boolean aceptarPressed;
    private boolean socketLost;
    
    private Thread hiloFlujoEntrada;
    
    public Principal() {
        initComponents();
        
        aceptarPressed = false;
        
        btnAceptarConfig.setVisible(false);
        btnEnviar.setVisible(false);
        txtMensaje.setVisible(false);
        
        grupoRadio.add(radCliente);
        grupoRadio.add(radServidor);
        
        
        
        //Metdo en caso de cerrar el programa bruscamente cierre todos los recursos
        cerrarRecursos();
        
        //Radio Buttons Events
        radServidor.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                txtIP.setEditable(true);
                txtPort.setEditable(true);
                txtIP.setText("localhost");
                tipoConexion = true;
                if(!btnAceptarConfig.isVisible())
                    btnAceptarConfig.setVisible(true);
            }
        });
        
        radCliente.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                txtIP.setEditable(true);
                txtPort.setEditable(true);
                txtIP.setText("");
                tipoConexion = false;
                if(!btnAceptarConfig.isVisible())
                    btnAceptarConfig.setVisible(true);
            }
        });
    }

    public void initServidor(int port){
           hiloFlujoEntrada = new Thread(new Runnable(){

            @Override
            public void run() {
                while(!socketLost){
                    flujoEntrada();
                }
            }
        });
        
        Thread hiloEsperaConexion = new Thread(new Runnable(){

            @Override
            public void run() {
                try {
                    btnEnviar.setEnabled(false);
                    socket = serverSocket.accept();
                    mensajesTextArea(1);
                    btnEnviar.setEnabled(true);
                    socketLost = false;
                    flujoEntrada = new DataInputStream(socket.getInputStream());
                    flujoSalida = new DataOutputStream(socket.getOutputStream());
                    hiloFlujoEntrada.start();
                }catch(SocketException e){
                    e.printStackTrace();
                }catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        try{
            serverSocket = new ServerSocket(port);
            hiloEsperaConexion.start();
        }catch(BindException e){
            mensajesTextArea(3);
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public void initCliente(String servidor, int port){        
        hiloFlujoEntrada = new Thread(new Runnable(){

            @Override
            public void run() {
                while(!socketLost){
                    flujoEntrada();
                }
            }
        });
        
        try{
            socket = new Socket(servidor, port);
            flujoEntrada = new DataInputStream(socket.getInputStream());
            flujoSalida = new DataOutputStream(socket.getOutputStream());
            socketLost = false;
            hiloFlujoEntrada.start();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public void mensajesTextArea(int op){
        switch(op){
            case 0:
                txtAreaMensajes.append("[*] Esperando conexión\n");
                break;
            case 1:
                txtAreaMensajes.append("[*] Conexion establecida con: "+socket.getInetAddress()+"\n");
                break;
            case 2:
                txtAreaMensajes.append("[*] Se ha perdido la conexión\n");
                break;
            case 3:
                txtAreaMensajes.append("[*] El puerto ya esta en uso\n");
                break;
            case 4:
                txtAreaMensajes.append("[*] Imposible conectararse\n");
                break;
            case 5:
                txtAreaMensajes.append("[*] Se ha cerrado o perdido la conexión\n");
                break;
        }
    }
    
    /*
        No logre encontrar la forma mas eficaz para saber si se ha perdido conexion con
        el cliente o servidor, flujoEntrada.readUTF() lanza una excepcion "EOFException"
        en caso de que esto sucediera, la variable "socketLost" sirve para que el hilo "hiloFlujoEntrada"
        acabara ya que este se cicla siempre y cuando socketLost sea falso.
    */
    public void flujoEntrada(){
        try {
            String mensaje = flujoEntrada.readUTF();
            txtAreaMensajes.append("El: "+mensaje+"\n");
            txtAreaMensajes.setCaretPosition(txtAreaMensajes.getDocument().getLength()); //auto scroll down
        }catch(EOFException e){
            e.printStackTrace();
            aceptarPressed = true;
            abrirOcerrarConexion(); //
        }catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void abrirOcerrarConexion(){
        if(!aceptarPressed){
            txtAreaMensajes.setText("");
            if(tipoConexion){
                initServidor(Integer.parseInt(txtPort.getText()));
                if(serverSocket == null)
                    return;
                mensajesTextArea(0);
            }else{
                initCliente(txtIP.getText(), Integer.parseInt(txtPort.getText()));
                if(socket == null){
                    mensajesTextArea(4);
                    return;
                }
                mensajesTextArea(1);
            }
            aceptarPressed = true;
            btnEnviar.setVisible(true);
            txtMensaje.setVisible(true);
            btnAceptarConfig.setText("Cerrar");
            
            
        }else{
            try{
                mensajesTextArea(5);
                socketLost = true;
                btnEnviar.setVisible(false);
                txtMensaje.setVisible(false);
                btnAceptarConfig.setText("Aceptar");
                aceptarPressed = false;
                if(tipoConexion){
                    serverSocket.close();
                    serverSocket = null;
                }
                if(socket != null){
                    socket.close();
                    socket = null;
                }
                    
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
    
    public void cerrarRecursos(){
        addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent evt){
                if(aceptarPressed)
                    abrirOcerrarConexion();
                System.exit(0);
            }
        });
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        grupoRadio = new javax.swing.ButtonGroup();
        txtIP = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        txtPort = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        radCliente = new javax.swing.JRadioButton();
        radServidor = new javax.swing.JRadioButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtAreaMensajes = new javax.swing.JTextArea();
        txtMensaje = new javax.swing.JTextField();
        btnEnviar = new javax.swing.JButton();
        btnAceptarConfig = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Principal");

        txtIP.setEditable(false);

        jLabel1.setText("Direccion IP:");

        txtPort.setEditable(false);

        jLabel2.setText("Numero puerto:");

        radCliente.setText("Ser Cliente");

        radServidor.setText("Ser servidor");

        txtAreaMensajes.setEditable(false);
        txtAreaMensajes.setColumns(20);
        txtAreaMensajes.setRows(5);
        jScrollPane1.setViewportView(txtAreaMensajes);

        btnEnviar.setText("Enviar");
        btnEnviar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnviarActionPerformed(evt);
            }
        });

        btnAceptarConfig.setText("Aceptar");
        btnAceptarConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAceptarConfigActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(txtMensaje)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEnviar))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtIP, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(txtPort, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(btnAceptarConfig))))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(13, 13, 13)
                                .addComponent(radServidor)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(radCliente)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radCliente)
                    .addComponent(radServidor))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(btnAceptarConfig))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtMensaje, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEnviar))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnEnviarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEnviarActionPerformed
        try {
            String mensaje = txtMensaje.getText();
            txtMensaje.setText("");
            flujoSalida.writeUTF(mensaje);
            txtAreaMensajes.append("Tu: "+mensaje+"\n");
            txtAreaMensajes.setCaretPosition(txtAreaMensajes.getDocument().getLength()); //auto scroll down
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }//GEN-LAST:event_btnEnviarActionPerformed

    private void btnAceptarConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAceptarConfigActionPerformed
        abrirOcerrarConexion();
    }//GEN-LAST:event_btnAceptarConfigActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Principal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Principal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Principal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Principal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Principal().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAceptarConfig;
    private javax.swing.JButton btnEnviar;
    private javax.swing.ButtonGroup grupoRadio;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JRadioButton radCliente;
    private javax.swing.JRadioButton radServidor;
    private javax.swing.JTextArea txtAreaMensajes;
    private javax.swing.JTextField txtIP;
    private javax.swing.JTextField txtMensaje;
    private javax.swing.JTextField txtPort;
    // End of variables declaration//GEN-END:variables
}
