package org.meveo.api.dto.cpq;

import org.meveo.api.dto.response.SearchResponse;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Tarik FAKHOURI
 * @version 10.0
 */
@SuppressWarnings("serial")
@XmlRootElement(name = "MediaListResponsDto")
@XmlAccessorType(XmlAccessType.FIELD)
public class MediaListResponsDto extends SearchResponse {


	
    /** list of media **/
    private MediaListDto media;


	/**
	 * @return media
	 */
	public MediaListDto getMedia() {
		if(media == null)
			media = new MediaListDto();
		return media;
	}

	/**
	 * @param MediaListDto media
	 */
	public void setMedia(MediaListDto media) {
		this.media = media;
	}
	
	
	
}
