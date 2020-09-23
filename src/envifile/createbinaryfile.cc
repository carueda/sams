#include <stdio.h>
#include <assert.h>


static char* filename = "binary.data";

	
//	This program creates a binary file with different envi
//	data types. The first byte indicates the byte order of
//	the data: 0=little endian, 1=bigendian (which will
//	depend on native underlying machine). Then for each
//	value, two token are written: first a byte indicating
//	the envi data type, and then the corresponding value.
//  See EnviDataType.main()
//  $Id$

int main() {
	
	printf("creating %s\n", filename);
	FILE* file = fopen(filename, "w");
	assert(file);
	
	// first determine the native byte order:
	long lon = 1;
	char* b = (char*) &lon;
	char byte_order = (*b == 1)? 0 : 1;
	
	printf("native byte order = %d\n", (int) byte_order);
	assert(1 == fwrite(&byte_order, 1, sizeof(byte_order), file)); 
	
	
	char data_type;

	// write a byte value
	data_type = 1;
	unsigned char byte_ = 210;
	printf("%d byte\n", (int) byte_);
	assert(1 == fwrite(&data_type, 1, sizeof(data_type), file)); 
	assert(1 == fwrite(&byte_, 1, sizeof(byte_), file)); 

	// write an int16 value
	data_type = 2;
	short short_ = 30021;
	printf("%d int16\n", (int) short_);
	assert(1 == fwrite(&data_type,   sizeof(data_type),   1, file)); 
	assert(1 == fwrite(&short_, sizeof(short_), 1, file)); 
	
	
	// write an uint16 value
	data_type = 12;
	unsigned short ushort_ = 60021;
	printf("%d uint16\n", (int) ushort_);
	assert(1 == fwrite(&data_type,   sizeof(data_type),   1, file)); 
	assert(1 == fwrite(&ushort_, sizeof(ushort_), 1, file)); 
	
	
	// write an int32 value
	data_type = 3;
	int int_ = 2147483647;
	printf("%d int32\n", int_);
	assert(1 == fwrite(&data_type,   sizeof(data_type),   1, file)); 
	assert(1 == fwrite(&int_, sizeof(int_), 1, file)); 
	
	
	// write an uint32 value
	data_type = 13;
	unsigned int uint_ = (unsigned int) 2147483647 + 1;
	printf("%u uint32\n", (int) uint_);
	assert(1 == fwrite(&data_type,   sizeof(data_type),   1, file)); 
	assert(1 == fwrite(&uint_, sizeof(uint_), 1, file)); 
	
	
	// write a float value
	data_type = 4;
	float float_ = 142857.0;
	printf("%f float\n", float_);
	assert(1 == fwrite(&data_type,   sizeof(data_type),   1, file)); 
	assert(1 == fwrite(&float_, sizeof(float_), 1, file)); 
	
	
	// write a double value
	data_type = 5;
	double double_ = 142857.142857;
	printf("%f double\n", double_);
	assert(1 == fwrite(&data_type,   sizeof(data_type),   1, file)); 
	assert(1 == fwrite(&double_, sizeof(double_), 1, file)); 
	
	
	fclose(file);
	
	return 0;
}
