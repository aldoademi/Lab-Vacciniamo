
import java.sql.*;

//RISORSA CONDIVISA CHE SI INTERFACCIERA' LOCALMENTE CON IL NOSTRO SKELETON e CON IL DB per eseguire query
public class EsecutoreQuery implements SkeletonInterface{
	
	private Connection connessione;
	private Statement istruzione;
	private ResultSet rs;
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
	


	//registra nel database il centro vaccinale
	public synchronized int registraCentroVaccinale(String nome, String qualificatore, String indirizzo, String numeroCivico, String comune, String provincia, String Cap, String tipologia) throws SQLException {
		int ret = 0;
		String queryPerIdMax = "SELECT i.id  FROM indirizzo i WHERE i.id >=ALL(SELECT id FROM indirizzo)";
		ResultSet rs;
		int brs = 0;
		rs = istruzione.executeQuery(queryPerIdMax);
		
		int idIndirizzoDaAggiungere = 0;
		
		while(rs.next()) {
			idIndirizzoDaAggiungere = rs.getInt("id") + 1;
		}
		
		String queryPerVerificareEsistenzaCentro = "SELECT nome FROM centrivaccinali WHERE nome = '"+ nome +"';";
		rs = istruzione.executeQuery(queryPerVerificareEsistenzaCentro);
		
		if(rs.next()==false) {
			String queryPerVerificareEsistenzaIndirizzo = "SELECT id FROM indirizzo WHERE nome = '"+ indirizzo +"' AND comune = '"+ comune +"' AND cap = '"+ Cap +"'; ";
			rs = istruzione.executeQuery(queryPerVerificareEsistenzaIndirizzo);
			
			if(rs.next()==false) {
				
				String queryPerInserireIndirizzo = "INSERT INTO indirizzo VALUES ('"+ idIndirizzoDaAggiungere +"', '"+qualificatore+"', '"+ indirizzo +"', '"+ numeroCivico +"', '"+ comune +"', '"+ Cap +"', '"+ provincia +"');";
				brs = istruzione.executeUpdate(queryPerInserireIndirizzo);
				String queryPerInserireCentro = "INSERT INTO centrivaccinali VALUES ('"+ nome +"', '"+ tipologia +"', '"+ idIndirizzoDaAggiungere +"');";
				brs = istruzione.executeUpdate(queryPerInserireCentro);
			}
			else {
				int id = rs.getInt("id");
				String queryPerInserireCentro = "INSERT INTO centrivaccinali VALUES ('"+ nome +"', '"+ tipologia +"', "+ id +");";
				brs = istruzione.executeUpdate(queryPerInserireCentro);
			}
			
			ret = 1;
		}
		else {
			ret = -1; 
		}
		return ret;
	}

}
