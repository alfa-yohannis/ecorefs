if [ $# != 1 ]
then
	echo "Usage: $0 Bpmn2ElementName"
	exit 1
fi

TEMPLATE=Validator.java

sed -e "s/Bpmn2ElementName/$1/g" <${TEMPLATE} >$1${TEMPLATE}
