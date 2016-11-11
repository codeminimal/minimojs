package br.com.kodeless.minimojs;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;

public class XStringUtil {

	public static void main(String[] args) {
		System.out.println(generateHashMD5("1122"));
		System.out.println(generateHash());
		System.out.println(replaceFirst("asdf asdf {xbody} asdf asdf", "{xbody}", "qwer"));
	}

	public static String generateHashMD5(String value) {
		byte[] passBytes;
		passBytes = value.getBytes();
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
		}
		md.update(passBytes);
		byte[] hash = md.digest();
		return Base64.encodeBase64String(hash);
	}

	public static String generateHash() {
		// Determia as letras que poderao estar presente nas chaves
		String num = "1234567890ABCDEFGHIJKLMNOPQRSTUVXYWZ";

		Random random = new Random();

		String armazenaChaves = "";
		int index = -1;
		for (int i = 0; i < 10; i++) {
			index = random.nextInt(num.length());
			armazenaChaves += num.substring(index, index + 1);
		}
		return armazenaChaves;
	}

	public static String toHtmlEntities(String texto) {
		String result = texto.replaceAll("á", "&aacute;").replaceAll("é", "&eacute;").replaceAll("í", "&iacute;")
				.replaceAll("ó", "&oacute;").replaceAll("ú", "&uacute;").replaceAll("ç", "&ccedil;")
				.replaceAll("õ", "&otilde;").replaceAll("ã", "&atilde;").replaceAll("ê", "&ecirc;")
				.replaceAll("ô", "&ocirc;").replaceAll("à", "&agrave;");
		return result;
	}

	public static String toHexa(String texto) {
		String result = texto.replaceAll("&quot;", "\\\\x22").replaceAll("&amp;", "\\\\x26").replaceAll("&lt;", "\\\\x3C")
				.replaceAll("&gt;", "\\\\x3E").replaceAll("&lsquo;", "\\\\x2018").replaceAll("&rsquo;", "\\\\x2019")
				.replaceAll("&ldquo;", "\\\\x201C").replaceAll("&rdquo;", "\\\\x201D").replaceAll("&sbquo;", "\\\\x201A")
				.replaceAll("&bdquo;", "\\\\x201E").replaceAll("&prime;", "\\\\x2032").replaceAll("&Prime;", "\\\\x2033")
				.replaceAll("&nbsp;", "\\\\xA0").replaceAll("&ndash;", "\\\\x2013").replaceAll("&mdash;", "\\\\x2014")
				.replaceAll("&ensp;", "\\\\x2002").replaceAll("&emsp;", "\\\\x2003").replaceAll("&thinsp;", "\\\\x2009")
				.replaceAll("&brvbar;", "\\\\xA6").replaceAll("&bull;", "\\\\x2022").replaceAll("&hellip;", "\\\\x2026")
				.replaceAll("&circ;", "\\\\x2C6").replaceAll("&uml;", "\\\\xA8").replaceAll("&tilde;", "\\\\x2DC")
				.replaceAll("&lsaquo;", "\\\\x2039").replaceAll("&rsaquo;", "\\\\x203A").replaceAll("&laquo;", "\\\\xAB")
				.replaceAll("&raquo;", "\\\\xBB").replaceAll("&oline;", "\\\\x203E").replaceAll("&iquest;", "\\\\xBF")
				.replaceAll("&iexcl;", "\\\\xA1").replaceAll("&Agrave;", "\\\\xC0").replaceAll("&Aacute;", "\\\\xC1")
				.replaceAll("&Acirc;", "\\\\xC2").replaceAll("&Atilde;", "\\\\xC3").replaceAll("&Auml;", "\\\\xC4")
				.replaceAll("&Aring;", "\\\\xC5").replaceAll("&AElig;", "\\\\xC6").replaceAll("&Ccedil;", "\\\\xC7")
				.replaceAll("&Egrave;", "\\\\xC8").replaceAll("&Eacute;", "\\\\xC9").replaceAll("&Ecirc;", "\\\\xCA")
				.replaceAll("&Euml;", "\\\\xCB").replaceAll("&Igrave;", "\\\\xCC").replaceAll("&Iacute;", "\\\\xCD")
				.replaceAll("&Icirc;", "\\\\xCE").replaceAll("&Iuml;", "\\\\xCF").replaceAll("&ETH;", "\\\\xD0")
				.replaceAll("&Ntilde;", "\\\\xD1").replaceAll("&Ograve;", "\\\\xD2").replaceAll("&Oacute;", "\\\\xD3")
				.replaceAll("&Ocirc;", "\\\\xD4").replaceAll("&Otilde;", "\\\\xD5").replaceAll("&Ouml;", "\\\\xD6")
				.replaceAll("&Oslash;", "\\\\xD8").replaceAll("&OElig;", "\\\\x152").replaceAll("&Scaron;", "\\\\x160")
				.replaceAll("&Ugrave;", "\\\\xD9").replaceAll("&Uacute;", "\\\\xDA").replaceAll("&Ucirc;", "\\\\xDB")
				.replaceAll("&Uuml;", "\\\\xDC").replaceAll("&Yacute;", "\\\\xDD").replaceAll("&THORN;", "\\\\xDE")
				.replaceAll("&szlig;", "\\\\xDF").replaceAll("&agrave;", "\\\\xE0").replaceAll("&aacute;", "\\\\xE1")
				.replaceAll("&acirc;", "\\\\xE2").replaceAll("&atilde;", "\\\\xE3").replaceAll("&auml;", "\\\\xE4")
				.replaceAll("&aring;", "\\\\xE5").replaceAll("&aelig;", "\\\\xE6").replaceAll("&ccedil;", "\\\\xE7")
				.replaceAll("&egrave;", "\\\\xE8").replaceAll("&eacute;", "\\\\xE9").replaceAll("&ecirc;", "\\\\xEA")
				.replaceAll("&euml;", "\\\\xEB").replaceAll("&igrave;", "\\\\xEC").replaceAll("&iacute;", "\\\\xED")
				.replaceAll("&icirc;", "\\\\xEE").replaceAll("&iuml;", "\\\\xEF").replaceAll("&eth;", "\\\\xF0")
				.replaceAll("&ntilde;", "\\\\xF1").replaceAll("&ograve;", "\\\\xF2").replaceAll("&oacute;", "\\\\xF3")
				.replaceAll("&ocirc;", "\\\\xF4").replaceAll("&otilde;", "\\\\xF5").replaceAll("&ouml;", "\\\\xF6")
				.replaceAll("&oslash;", "\\\\xF8").replaceAll("&oelig;", "\\\\x153").replaceAll("&scaron;", "\\\\x161")
				.replaceAll("&ugrave;", "\\\\xF9").replaceAll("&uacute;", "\\\\xFA").replaceAll("&ucirc;", "\\\\xFB")
				.replaceAll("&uuml;", "\\\\xFC").replaceAll("&yacute;", "\\\\xFD").replaceAll("&yuml;", "\\\\xFF")
				.replaceAll("&thorn;", "\\\\xFE");
		return result;
	}

	public static boolean isEmpty(String s) {
		return s == null || s.trim().equals("");
	}

	public static String replaceFirst(String s, String r, String w) {
		int index = s.indexOf(r);
		if (index >= 0) {
			return s.substring(0, index) + w + s.substring(index + r.length());
		}
		return s;
	}

}
