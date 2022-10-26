import java.awt.event.*;
import javax.swing.JButton;

//classe che gestisce gli eventi generati dall'utente
public class ControllerImpl implements ActionListener{
	
	private View v;
	private Model m; 
	
	
	public ControllerImpl(View v, Model m) {
		this.v = v;
		this.m = m;
		
		//cattura bottoni		
		JButton back = this.v.getBack();
		back.addActionListener(this);
		
		JButton[] sceltaUtente = this.v.getSceltaTipoUtente();
		sceltaUtente[0].addActionListener(this);
		sceltaUtente[1].addActionListener(this);
		
		JButton[] funzionalitaOperatore = this.v.getBottoniFunzionalitaOperatore();
		funzionalitaOperatore[0].addActionListener(this);
		funzionalitaOperatore[1].addActionListener(this);
		
		JButton[] funzionalitaCittadino = this.v.getBottoniFunzionalitaCittadino();
		funzionalitaCittadino[0].addActionListener(this);
		funzionalitaCittadino[1].addActionListener(this);
		funzionalitaCittadino[2].addActionListener(this);
		
	}

	public void actionPerformed(ActionEvent e) {
		
		String source = null;
		Object dati = null;
		
		if(e.getSource() instanceof JButton) {
			JButton bottoneOrigine = (JButton) e.getSource(); //recupera origine --> quale bottone ho cliccato
			source = bottoneOrigine.getName();
		}
		
		String button = (String)source;
		
		if(button.equals("INDIETRO")) {
			dati = null; //non serve passare nulla
		}
		
		if(button.equals("OPERATORE")) {
			dati = null; //non serve passare nulla
		}
		
		if(button.equals("CITTADINO")) {
			dati = null; //non serve passare nulla
		}
		
		if(button.equals("REGISTRA NUOVO CENTRO")) {
			dati = null;
		}
		
		if(button.equals("REGISTRA NUOVO VACCINATO")) {
			dati = null;
		}
		
		if(button.equals("REGISTRATI")) {
			dati = null;
		}
		
		if(button.equals("INFO CENTRI VACCINALI")) {
			dati = null;
		}
		
		
		
		
		m.updateModel(source, null); //elabora dati sul bottone d'origine
		
	}

}
