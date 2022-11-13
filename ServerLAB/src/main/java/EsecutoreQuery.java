
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

//RISORSA CONDIVISA CHE SI INTERFACCIERA' LOCALMENTE CON IL NOSTRO SKELETON e CON IL DB per eseguire query
public class EsecutoreQuery implements SkeletonInterface{
	
	private Connection connessione;
	private Statement istruzione;
	private ResultSet rs;
	private boolean result;
	private ViewInterface view;
	
	//costruttore che instanzia connessione a database
	public EsecutoreQuery(String username, String password, String host, String port, String nomeDB) {
		try {
			this.connessione = DataBaseConnessione.getConnection(username, password, host, port , nomeDB); //prende connessione al database
			this.istruzione = (Statement) connessione.createStatement(); //statement per eseguire query
			
		} catch (SQLException e) {
			System.err.println("ESECUTORE QUERY: connessione al DB non riuscita " + e.toString());
		} 
	}
	
	
	public synchronized void creazioneTabelle() throws SQLException {
		
		System.out.println("ESECUTORE QUERY: Inizializzo DataBase");
		
		String queryCreazione
				= "CREATE TABLE if not exists Cittadini (\n"
				+ "	codiceFiscale CHARACTER(16),\n"
				+ "	cognome CHARACTER(30) NOT NULL,\n"
				+ "	nome CHARACTER(30) NOT NULL,\n"
				+ "	PRIMARY KEY (codiceFiscale)\n"
				+ ");"
		
				+	"CREATE TABLE if not exists Indirizzo (\n"
				+ "	id NUMERIC,		\n"
				+ "	qualificatore CHARACTER(6) NOT NULL CHECK(qualificatore IN ('Via', 'Viale', 'Piazza')),\n"
				+ "	nome CHARACTER(40) NOT NULL, \n"
				+ "	numeroCivico CHARACTER(6) NOT NULL,\n"
				+ "	comune CHARACTER(30) NOT NULL,\n"
				+ "	cap NUMERIC NOT NULL CHECK(cap BETWEEN 0 AND 99999),\n"
				+ "	siglaProvincia CHARACTER(2) NOT NULL, \n"
				+ "	PRIMARY KEY (id)\n"
				+ ");"
		
				+ "CREATE TABLE if not exists CentriVaccinali (\n"
				+ "	nome CHARACTER(40),\n"
				+ "	tipologia CHARACTER(11) NOT NULL CHECK(tipologia IN ('Ospedaliero', 'Hub', 'Aziendale')),\n"
				+ "	idIndirizzo NUMERIC NOT NULL REFERENCES Indirizzo(id),\n"
				+ "	PRIMARY KEY (nome)\n"
				+ ");"
		
				+ "CREATE TABLE if not exists Vaccinazione (\n"
				+ "	id SMALLINT,\n"
				+ "	codiceFiscale CHARACTER(16) REFERENCES Cittadini(codiceFiscale),\n"
				+ "	data DATE NOT NULL, \n"
				+ "	tipoVaccino CHARACTER(11) NOT NULL CHECK(tipoVaccino IN ('Pfizer', 'Moderna', 'J&J', 'AstraZeneca')),\n"
				+ "	nomeCentro CHARACTER(40) NOT NULL REFERENCES CentriVaccinali(nome),\n"
				+ "	nDosi CHARACTER(20) CHECK (nDosi IN ('Prima', 'Seconda', 'Terza o Successiva')),\n"
				+ "	PRIMARY KEY (id)\n"
				+ ");"
		
				+ "CREATE TABLE if not exists Cittadini_Registrati (\n"
				+ "	codiceFiscale CHARACTER(16) REFERENCES Cittadini(codiceFiscale),\n"
				+ "	username CHARACTER(50) NOT NULL, \n"
				+ "	password CHARACTER(50) NOT NULL,\n"
				+ "	email CHARACTER(60) NOT NULL,\n"
				+ "	idVaccinazione SMALLINT UNIQUE REFERENCES Vaccinazione(id),\n"
				+ "	PRIMARY KEY(codiceFiscale)\n"
				+ ");"
		
				+ "CREATE TABLE if not exists Eventi_Avversi (\n"
				+ "	codiceFiscale CHARACTER(16) REFERENCES Cittadini_Registrati(codiceFiscale),\n"
				+ "	evento CHARACTER(30),\n"
				+ "	severita NUMERIC CHECK(severita BETWEEN 1 AND 5),\n"
				+ "	note CHARACTER(256),\n"
				+ "	nomeCentro CHARACTER(40) REFERENCES CentriVaccinali(nome), \n"
				+ "	PRIMARY KEY (codiceFiscale, evento)\n"
				+ ")";

			result = istruzione.execute(queryCreazione);
		
	}

	//registra nel database il centro vaccinale --> METODI SYCHRONIZED, si accede ai dati in modo concorrente
	public synchronized int registraCentroVaccinale(String nome, String qualificatore, String indirizzo, String numeroCivico, String comune, String provincia, String Cap, String tipologia) throws SQLException {
		int ret = 0;
		String queryUltimoID = "SELECT i.id  FROM indirizzo i WHERE i.id >=ALL(SELECT id FROM indirizzo)";
		
		ResultSet rs;
		int brs = 0;
		
		rs = istruzione.executeQuery(queryUltimoID);
		
		int idIndirizzoNuovo = 0;
		
		while(rs.next()) {
			idIndirizzoNuovo = rs.getInt("id") + 1; //aggioramento id indirizzo progressivo
		}
		
		String queryPerVerificareEsistenzaCentro = "SELECT nome FROM centrivaccinali WHERE nome = '"+ nome +"';";
		rs = istruzione.executeQuery(queryPerVerificareEsistenzaCentro);
		
		if(rs.next()==false) {
			String queryPerVerificareEsistenzaIndirizzo = "SELECT id FROM indirizzo WHERE nome = '"+ indirizzo +"' AND comune = '"+ comune +"' AND cap = '"+ Cap +"'; ";
			rs = istruzione.executeQuery(queryPerVerificareEsistenzaIndirizzo);
			
			if(rs.next()==false) {
				
				String queryPerInserireIndirizzo = "INSERT INTO indirizzo VALUES ('"+ idIndirizzoNuovo +"', '"+qualificatore+"', '"+ indirizzo +"', '"+ numeroCivico +"', '"+ comune +"', '"+ Cap +"', '"+ provincia +"');";
				brs = istruzione.executeUpdate(queryPerInserireIndirizzo);
				String queryPerInserireCentro = "INSERT INTO centrivaccinali VALUES ('"+ nome +"', '"+ tipologia +"', '"+ idIndirizzoNuovo +"');";
				brs = istruzione.executeUpdate(queryPerInserireCentro);
			}
			else {
				int id = rs.getInt("id");
				String queryPerInserireCentro = "INSERT INTO centrivaccinali VALUES ('"+ nome +"', '"+ tipologia +"', "+ id +");";
				brs = istruzione.executeUpdate(queryPerInserireCentro);
			}
			
			ret = 1; //operazione e buon fine
		}
		else {
			ret = -1; //operazione non a buon fine
		}
		
		return ret;
	}


	//metodo che permette di registrare un vaccinato nel DB
	public synchronized int registraVaccinato(String nomeCentro, String nome, String cognome, String codiceFiscale,String dataSomministrazione, String tipoVaccino, String nDosi) throws SQLException {
		int ret = 0;
		
		//cerca l'ultimo identificativo
		String queryPerIdMax = "SELECT id FROM vaccinazione WHERE id >= ALL(SELECT id FROM vaccinazione)";
		ResultSet rs = istruzione.executeQuery(queryPerIdMax);
		int brs;
		int idVacMax = 0;
		
		//genera identificativo univoco per il vaccinato
		while(rs.next()) {
			idVacMax = rs.getInt("id") + 1;
		}
		
		String queryPerVerificareEsistenzaCittadino = "SELECT * FROM cittadini WHERE codicefiscale = '"+ codiceFiscale +"'";
		rs = istruzione.executeQuery(queryPerVerificareEsistenzaCittadino);
		
		//se il cittadino non è nel DB
		if(rs.next()==false) {
			String queryPerVerificareVaccinato = "SELECT * FROM vaccinazione WHERE codiceFiscale = '"+ codiceFiscale +"'";
			rs = istruzione.executeQuery(queryPerVerificareVaccinato);
			
			//se non è nella tabella dei vaccinati, lo aggiungo sia nei cittadini che nei vaccinati
			if(rs.next()==false) {
				System.out.println("ESECUTORE QUERY: aggiungo: " + nomeCentro + " " + nome + " " + cognome + " " + codiceFiscale + " " + dataSomministrazione + " " + tipoVaccino + " " + nDosi);
				String queryPerInserireCittadino = "INSERT INTO cittadini VALUES ('"+codiceFiscale+"', '"+cognome+"', '"+nome+"');";
				brs = istruzione.executeUpdate(queryPerInserireCittadino);
				System.out.println("cittadino ok aggiunto");
				
				String queryPerInserireVaccinato = "SET datestyle = \"ISO, DMY\";INSERT INTO vaccinazione VALUES('"+ idVacMax +"', '"+ codiceFiscale+"', '"+ dataSomministrazione +"', '"+ tipoVaccino +"', '"+ nomeCentro+"' , '"+ nDosi+"')";
				brs = istruzione.executeUpdate(queryPerInserireVaccinato);
				System.out.println("vaccinato ok aggiunto");
				ret = 1; //operazione e buon fine
			}
			else {
				ret = -1;
			}
		}
		
		else {
			String queryPerVerificareVaccinato = "SELECT * FROM vaccinazione WHERE codiceFiscale = '"+ codiceFiscale +"'";
			rs = istruzione.executeQuery(queryPerVerificareVaccinato);
			if(rs.next()==false) {
				String queryPerInserireVaccinato = "INSERT INTO vaccinazione VALUES('"+ idVacMax +"', '"+ codiceFiscale+"', '"+ dataSomministrazione  +"', '"+ tipoVaccino +"', '"+ nomeCentro+"' , '"+ nDosi+"')";
				brs = istruzione.executeUpdate(queryPerInserireVaccinato);
				ret = 1; //operazione e buon fine
			}
			else {
				ret = 0;
			}
		}
		
		return ret;
	}


	//questo metodo Cerca nel DB : nome, cognome, id univoco, il nome del centro dove è stato vaccinato
	public synchronized List<String> IdUnivoco(String codiceFiscale) throws SQLException {
		List<String> ret = new ArrayList<String>();
		
		String queryRicerca = "SELECT nome, cognome, id, nomeCentro FROM cittadini c JOIN vaccinazione v ON c.codiceFiscale = v.codiceFiscale WHERE c.codicefiscale = '"+codiceFiscale+"'";
		ResultSet rs = istruzione.executeQuery(queryRicerca);
		
		while(rs.next()) {
			ret.add(rs.getString("nome"));
			ret.add(rs.getString("cognome"));
			ret.add(String.valueOf(rs.getInt("id"))); //indice 2
			ret.add(rs.getString("nomecentro"));
		}
		
		return ret;
	}


	//questo metodo ritorna i centri vaccinali aggiunti al DB nella combox box
	public synchronized List<String> retElencoCentriVaccinali() throws SQLException {
		
		List<String> ret = new ArrayList<String>();
		
		String query = "SELECT nome FROM centrivaccinali ORDER BY nome ASC";
		ResultSet rs = istruzione.executeQuery(query);
		
		while(rs.next()) {
			ret.add(rs.getString("nome"));
		}
		
		return ret;
	}


	//cerca nel DB il centro selezionato
	public synchronized boolean esisteCentroNome(String nomeCentro) throws SQLException {
		
		boolean ret = false;
		String query = "SELECT nome FROM centrivaccinali WHERE nome = '"+ nomeCentro +"';";
		ResultSet rs = istruzione.executeQuery(query);
		
		while(rs.next()) {
			ret = true;
		}
		
		return ret;
	}


	//cerca nel DB il cittadino vaccinato
	public synchronized boolean checkCittadinoVaccinato(String codiceFiscale) throws SQLException {
		
		String query = "SELECT * \n"
				+ "FROM cittadini c JOIN vaccinazione v ON c.codicefiscale = v.codicefiscale\n"
				+ "WHERE c.codicefiscale = '"+ codiceFiscale +"'";
		
		ResultSet rs = this.istruzione.executeQuery(query);
		
		while(rs.next()) {
			return true;
		}
		return false;
	}


	//registra al portale cittadini l'utente vaccinato
	public synchronized int registrazioneCittadino(String nome, String cognome, String codiceFiscale, String eMail, String username, String password, String IdUnivoco) throws SQLException{
		int ret = 0;
		int brs;
		String queryControlloCittadino = "SELECT * FROM cittadini WHERE codiceFiscale = '"+ codiceFiscale +"'";
		ResultSet rs = istruzione.executeQuery(queryControlloCittadino);
		
		if(rs.next()) {
			String queryControlloRegistrato = "SELECT * FROM cittadini_registrati WHERE codiceFiscale = '"+ codiceFiscale +"'";
			rs = istruzione.executeQuery(queryControlloRegistrato);
			
			if(rs.next()==false) { 
				String queryInsert = "INSERT INTO cittadini_registrati VALUES('"+ codiceFiscale +"', '"+ username +"', '"+ password+"', '"+ eMail +"', '"+ IdUnivoco +"');";
				brs = istruzione.executeUpdate(queryInsert);
				System.out.println("cittadino registrato");
				ret = 1; //buon fine
			}
			
			else {
				System.out.println("Cittadino non inserito, già presente");
				ret = 0; //query buon fine ma cittadino non inserito
			}
		}
		else {
			System.out.println("cittadino fantastico");
			ret = -1;
		}
		return ret;
	}


	//cerca nel DB se trova un utente con lo stesso nome
	public synchronized boolean esisteUtente(String username) throws SQLException {
		boolean ret = false;
		String query = "SELECT username, password FROM cittadini_registrati WHERE username = '"+username+"'";
		ResultSet rs = istruzione.executeQuery(query);
		
		while(rs.next()) {
			ret = true;
		}
		return ret;
	}


	//verifica che id corrisponde ad utente registrato
	public synchronized boolean verificaCorrispondenzaId(String codiceFiscale, String idVaccinato) throws SQLException {
		
		String queryControlloId = "SELECT id FROM vaccinazione NATURAL JOIN cittadini\n"
				+ "WHERE codicefiscale = '"+ codiceFiscale +"'";
		rs = istruzione.executeQuery(queryControlloId);
		
		if(rs.next()) {
			String idQuery = rs.getString(1);
			System.out.println("L'id della query è: " + idQuery + ", l'id inserito dall'utente è: " + idVaccinato);
			
			if(idQuery.equals(idVaccinato)) {
				return true; //buon fine 
			}
			else {
				System.out.println("L'ID NON è UGUALE CAMBIARE!!!!");
				return false;
			}
		}
		
		else {return false;}
	}

}
