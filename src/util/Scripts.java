package util;

/**
 * This class contains all the shell scripts used by TAGOPSIN to extract coding 
 * sequences from the genome when parsing GenBank flat files (see also class GenBank.java).
 * 
 * @author 	Anisah W. Ghoorah, University of Mauritius
 * @since	2019-12-17
 * @version	1.0
 *
 */

public class Scripts {
	
	public static String getWholeSeq() {
		
		String s = "";
		
		s += "#!/bin/bash\n";
		s += "\n";
		s += "genbankfile=$1\n";
		s += "linenumber=$2\n";
		s += "\n";
		s += "wholesequence=$(cat $genbankfile |\n";
		s += "                tail -n +$linenumber |\n";
		s += "                sed '1d' |\n";
		s += "                sed '$d' |\n";
		s += "                sed '$d' |\n";
		s += "                awk '{$1=$1};1' |\n";
		s += "                cut -d ' ' -f 2-7 |\n";
		s += "                sed 's/[[:blank:]]//g' |\n";
		s += "                tr -d '\\n' )\n";
		s += "\n";
		s += "echo $wholesequence";
		
		return s;
	}
	
	public static String getSeq1() {
		
		String s = "";
		
		s += "#!/bin/bash\n";
		s += "\n";
		s += "seqbinfile=$1\n";
		s += "start=$2\n";
		s += "stop=$3\n";
		s += "\n";
		s += "sequence=$(cat $seqbinfile | cut -b $start-$stop)\n";
		s += "\n";
		s += "echo $sequence";
		
		return s;
	}
	
	public static String getSeq2() {
		
		String s = "";
		
		s += "#!/bin/bash\n";
		s += "\n";
		s += "seqbinfileone=$1\n";
		s += "seqbinfiletwo=$2\n";
		s += "start=$3\n";
		s += "stop=$4\n";
		s += "\n";
		s += "sequence=$(cat $seqbinfileone $seqbinfiletwo | cut -b $start-$stop)\n";
		s += "\n";
		s += "echo $sequence";
		
		return s;
	}
	
	public static String getSeqLen() {
		
		String s = "";
		
		s += "#!/bin/bash\n";
		s += "\n";
		s += "genbankfile=$1\n";
		s += "linenumber=$2\n";
		s += "\n";
		s += "wholesequence=$(cat $genbankfile |\n";
		s += "                tail -n +$linenumber |\n";
		s += "                sed '1d' |\n";
		s += "                sed '$d' |\n";
		s += "                sed '$d' |\n";
		s += "                awk '{$1=$1};1' |\n";
		s += "                cut -d ' ' -f 2-7 |\n";
		s += "                sed 's/[[:blank:]]//g' |\n";
		s += "                tr -d '\\n')\n";
		s += "\n";
		s += "wholesequencelength=${#wholesequence}\n";
		s += "\n";
		s += "echo $wholesequencelength";
		
		return s;
	}
	
	public static String subseq() {
		
		String s = "";
		
		s += "#!/bin/bash\n";
		s += "\n";
		s += "genbankfile=$1\n";
		s += "linenumber=$2\n";
		s += "start=$3\n";
		s += "stop=$4\n";
		s += "filesubseq=$5\n";
		s += "\n";
		s += "sequence=$(cat $genbankfile |\n";
		s += "           tail -n +$linenumber |\n";
		s += "           sed '1d' |\n";
		s += "           sed '$d' |\n";
		s += "           sed '$d' |\n";
		s += "           awk '{$1=$1};1' |\n";
		s += "           cut -d ' ' -f 2-7 |\n";
		s += "           sed 's/[[:blank:]]//g' |\n";
		s += "           tr -d '\\n' |\n";
		s += "           cut -b $start-$stop)\n";
		s += "\n";
		s += "echo $sequence > $filesubseq";
		
		return s;
	}

}
